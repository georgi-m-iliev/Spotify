package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.ClientRequest;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.command.Command;
import bg.sofia.uni.fmi.mjt.spotify.server.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.SongLoadingFailureException;
import bg.sofia.uni.fmi.mjt.spotify.server.songs.SongHandler;
import bg.sofia.uni.fmi.mjt.spotify.server.users.LocalUserStorage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class SpotifyServer {
    private static final int BUFFER_SIZE = 1024;
    private static final String USERS_FILE = "user.json";
    private static final Gson gson = new Gson();

    private final CommandExecutor commandExecutor;

    private final String host;
    private final int port;
    private boolean isServerWorking;

    private ByteBuffer buffer;
    private Selector selector;
    private final LocalUserStorage users;
    private final SongHandler songHandler;
    private final ExecutorService executor;

    public SpotifyServer(String host, int port, Path songsDirPath) throws SongLoadingFailureException {
        this.host = host;
        this.port = port;
        this.songHandler = new SongHandler(songsDirPath);
        this.executor = Executors.newCachedThreadPool();
        this.users = new LocalUserStorage(Path.of(USERS_FILE), songHandler.getSongs());
        this.commandExecutor = new CommandExecutor(users, songHandler.getSongs(), executor);
    }

    public void start() {
        new Thread(() -> {
            SpotifyLogger.getLogger().log(Level.INFO, "Server starting up");
            try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                selector = Selector.open();
                configureServerSocketChannel(serverSocketChannel, selector);
                buffer = ByteBuffer.allocate(BUFFER_SIZE);
                isServerWorking = true;
                while (isServerWorking) {
                    try {
                        int readyChannels = selector.select();
                        if (readyChannels == 0) {
                            continue;
                        }

                        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                        processClientRequest(keyIterator);
                    } catch (ClosedChannelException e) {
                        SpotifyLogger.getLogger().log(Level.FINE, "SocketChannel has been closed.");
                    } catch (IOException e) {
                        SpotifyLogger.getLogger().log(
                                Level.WARNING,
                                String.format("Error occurred while processing client request: %s", e.getMessage()),
                                e);
                        System.out.println("Error occurred while processing client request: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                SpotifyLogger.getLogger().log(
                        Level.SEVERE,
                        String.format("Error occurred while starting server: %s", e.getMessage()),
                        e);
            }
        }).start();
    }

    public void stop() {
        SpotifyLogger.getLogger().log(Level.INFO, "Server shutting down...");
        this.isServerWorking = false;
        if (selector.isOpen()) {
            selector.wakeup();
        }
        executor.shutdown();
        users.close();
        songHandler.flush();
        SpotifyLogger.getLogger().log(Level.INFO, "Server has been shut down.");
    }

    private void processClientRequest(Iterator<SelectionKey> keyIterator) throws IOException {
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            if (key.isReadable()) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                String clientInput;
                try {
                    clientInput = getClientInput(clientChannel);
                } catch (IOException e) {
                    if (e.getMessage().equals("Connection reset")) {
                        SpotifyLogger.getLogger().log(
                                Level.INFO,
                                String.format("Client %s has forcibly disconnected.", clientChannel.getRemoteAddress()));
                    }
                    disposeSocket(clientChannel, key, keyIterator);
                    continue;
                }

                if (clientInput == null) {
                    continue;
                }


                ClientRequest request;
                try {
                    request = gson.fromJson(clientInput, ClientRequest.class);
                } catch (JsonSyntaxException e) {
                    SpotifyLogger.getLogger().log(
                            Level.WARNING,
                            String.format("Error occurred while parsing client request: %s", e.getMessage()),
                            e);
                    disposeSocket(clientChannel, key, keyIterator);
                    continue;
                }

                if (request.command().equals("disconnect")) {
                    System.out.println("Client has requested a disconnect: " + clientChannel.getRemoteAddress());
                    SpotifyLogger.getLogger().log(
                            Level.INFO,
                            String.format("Client %s has disconnected willfully.", clientChannel.getRemoteAddress()));
                    disposeSocket(clientChannel, key, keyIterator);
                    continue;
                }

                CommandResponse response;
                try {
                    Command command = Command.creator()
                            .command(request.command().stripTrailing())
                            .accessKey(request.accessKey())
                            .originAddress(clientChannel.socket().getInetAddress())
                            .originSocket(clientChannel.getRemoteAddress())
                            .build();
                    response = commandExecutor.execute(command);
                } catch (InvalidCommandException e) {
                    SpotifyLogger.getLogger().log(
                            Level.WARNING,
                            String.format("Client %s has sent invalid command %s.",
                                    clientChannel.getRemoteAddress(),
                                    request.command()));
                    response = CommandResponse.builder().buildError("Invalid command.");
                } catch (Exception e) {
                    SpotifyLogger.getLogger().log(
                            Level.SEVERE,
                            String.format("Error occurred while executing command: %s", e.getMessage()),
                            e);
                    response = CommandResponse.builder().buildError("An error occurred while executing the command.");
                }

                writeClientOutput(clientChannel, response);
            } else if (key.isAcceptable()) {
                accept(selector, key);
            }

            keyIterator.remove();
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(this.host, this.port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void disposeSocket(SocketChannel clientChannel, SelectionKey key, Iterator<SelectionKey> keyIterator) throws IOException {
        clientChannel.close();
        key.cancel();
        keyIterator.remove();
        SpotifyLogger.getLogger().log(
                Level.INFO,
                String.format("Client %s has disconnected.", clientChannel.getRemoteAddress()));
    }

    private String getClientInput(SocketChannel clientChannel) throws IOException {
        buffer.clear();
        int readBytes = clientChannel.read(buffer);
        if (readBytes < 0) {
            clientChannel.close();
            return null;
        }

        buffer.flip();
        byte[] clientInputBytes = new byte[buffer.remaining()];
        buffer.get(clientInputBytes);

        return new String(clientInputBytes, StandardCharsets.UTF_8);
    }

    private void writeClientOutput(SocketChannel clientChannel, CommandResponse response) throws IOException {
        buffer.clear();
        buffer.put(gson.toJson(response).getBytes());
        buffer.put(System.lineSeparator().getBytes());
        buffer.flip();
        clientChannel.write(buffer);
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = serverSocketChannel.accept();
        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);
    }

}