package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.ClientRequest;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.server.command.Command;
import bg.sofia.uni.fmi.mjt.spotify.server.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.SongLoadingFailureException;
import bg.sofia.uni.fmi.mjt.spotify.server.songs.SongLoader;
import bg.sofia.uni.fmi.mjt.spotify.server.users.LocalUserStorage;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpotifyServer {
    private static final int BUFFER_SIZE = 1024;
    private static final String HOST = "192.168.1.101";
    private static final Gson gson = new Gson();

    private final CommandExecutor commandExecutor;

    private final int port;
    private boolean isServerWorking;

    private ByteBuffer buffer;
    private Selector selector;
    private final LocalUserStorage users;
    private final ExecutorService executor;

    public SpotifyServer(int port, Path songsDirPath) throws SongLoadingFailureException {
        this.port = port;
        List<Song> availableSongs = SongLoader.loadSongs(songsDirPath);
        this.users = new LocalUserStorage(Path.of("users.txt"), availableSongs);
        this.executor = Executors.newCachedThreadPool();
        this.commandExecutor = new CommandExecutor(users, availableSongs, executor);
    }

    public void start() {
        new Thread(() -> {
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

                    } catch (IOException e) {
                        System.out.println("Error occurred while processing client request: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("failed to start server", e);
            }
        }).start();
    }

    public void stop() {
        this.isServerWorking = false;
        if (selector.isOpen()) {
            selector.wakeup();
        }
        executor.shutdown();
        users.close();
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
                    // TODO: Log client socket reset
                    System.out.println("Error occurred while reading client input: " + e.getMessage());
                    disposeSocket(clientChannel, key, keyIterator);
                    continue;
                }

                if (clientInput == null) {
                    continue;
                }

                System.out.println(clientInput);
                // TODO: handle wrong input, not properly formatted JSON
                ClientRequest request = gson.fromJson(clientInput, ClientRequest.class);

                if (request.command().equals("disconnect")) {
                    System.out.println("Client has requested a disconnect: " + clientChannel.getRemoteAddress());
                    // TODO: Log client disconnect
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
                    // TODO: Log invalid command
                    response = CommandResponse.builder()
                            .status("ERROR")
                            .message("Invalid command. Please try again!")
                            .build();
                }

                writeClientOutput(clientChannel, response);
            } else if (key.isAcceptable()) {
                accept(selector, key);
            }

            keyIterator.remove();
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(HOST, this.port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void disposeSocket(SocketChannel clientChannel, SelectionKey key, Iterator<SelectionKey> keyIterator) throws IOException {
        clientChannel.close();
        key.cancel();
        keyIterator.remove();
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