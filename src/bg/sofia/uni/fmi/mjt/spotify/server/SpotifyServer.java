package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.ClientRequest;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.server.command.Command;
import bg.sofia.uni.fmi.mjt.spotify.server.command.CommandCreator;
import bg.sofia.uni.fmi.mjt.spotify.server.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.spotify.server.exceptions.InvalidCommandException;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SpotifyServer {
    private static final int BUFFER_SIZE = 1024;
    private static final String HOST = "localhost";
    private static final Gson gson = new Gson();

    private final CommandExecutor commandExecutor;

    private final int port;
    private final Path songsDirPath;
    private boolean isServerWorking;

    private ByteBuffer buffer;
    private Selector selector;
    private List<Song> availableSongs;
    private LocalUserStorage users;

    public SpotifyServer(int port, Path songsDirPath) {
        this.port = port;
        this.songsDirPath = songsDirPath;
        this.availableSongs = new ArrayList<>();
        this.users = new LocalUserStorage(Path.of("users.txt"));
        this.commandExecutor = new CommandExecutor(users, availableSongs);
        loadSongs();
    }

    private void loadSongs() {
        if (!Files.exists(songsDirPath)) {
            throw new IllegalArgumentException("Nonexistent path for songs directory");
        }
        if (!Files.isDirectory(songsDirPath)) {
            throw new IllegalArgumentException("Invalid path for songs directory");
        }
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(songsDirPath)) {
            int ind = 0;
            for (Path songPath : dirStream) {
                if (!songPath.toFile().getName().endsWith(".wav")) {
                    continue;
                }
                availableSongs.add(
                    new Song(
                        ind++,
                        songPath.getFileName().toString().split("\\.")[0],
                        "unknown",
                        songPath)
                );
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load songs", e);
        }
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
                    System.out.println("Error occurred while reading client input: " + e.getMessage());
                    disposeSocket(clientChannel, key, keyIterator);
                    continue;
                }

                if (clientInput == null) {
                    continue;
                }

                // TODO: handle wrong input, not properly formatted JSON
                ClientRequest request = gson.fromJson(clientInput, ClientRequest.class);
                if (request.command().equals("disconnect")) {
                    System.out.println("Client has requested a disconnect: " + clientChannel.getRemoteAddress());
                    disposeSocket(clientChannel, key, keyIterator);
                    continue;
                }

                AccessKey accessKey = null;
                if (request.accessKey() != null) {
                    accessKey = request.accessKey();
                }

                try {
                    Command command = CommandCreator.newCommand(
                            request.command().stripTrailing(),
                            accessKey);
                    writeClientOutput(clientChannel, commandExecutor.execute(command));
                } catch (InvalidCommandException e) {
                    CommandResponse error = CommandResponse.builder()
                            .status("ERROR")
                            .message("Request has failed. Please try again!")
                            .build();
                    writeClientOutput(clientChannel, error);
                }

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

    private void disposeSocket(SocketChannel clientChannel, SelectionKey key, Iterator<SelectionKey> keyIterator) {
        try {
            clientChannel.close();
            key.cancel();
            keyIterator.remove();
        } catch (IOException e) {
            System.out.println("Error occurred while disposing socket: " + e.getMessage());
        }
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