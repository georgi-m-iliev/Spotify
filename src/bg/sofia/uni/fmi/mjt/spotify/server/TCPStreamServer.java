package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;

import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;

public class TCPStreamServer implements Runnable{
    private final int port;
    private final Path audioFilePath;

    public TCPStreamServer(int port, Path audioFilePath) {
        this.port = port;
        this.audioFilePath = audioFilePath;
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new java.net.InetSocketAddress(port));
            System.out.println("TCP stream server listening on port " + port);
            try (SocketChannel clientChannel = serverSocketChannel.accept();
                 FileInputStream fileInputStream = new FileInputStream(audioFilePath.toFile())) {
                System.out.println("TCP stream client connected: " + clientChannel.getRemoteAddress());

                ByteBuffer buffer = ByteBuffer.allocate(4096);
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer.array())) != -1) {
                    buffer.limit(bytesRead);
                    clientChannel.write(buffer);
                    buffer.clear();
                }
                System.out.println("TCP streaming finished.");
            } catch (IOException e) {
                if (e.getMessage().equals("Connection reset by peer")) {
                    SpotifyLogger.logger().info(String.format("TCP stream client %s disconnected while streaming",
                                                        e.getMessage()));
                } else {
                    SpotifyLogger.logger().warning(String.format("TCP streaming error while streaming file: %s",
                                                        e.getMessage()));
                }
            }
        } catch (IOException e) {
            // Error of starting server itself
            SpotifyLogger.logger().log(Level.SEVERE, "TCP stream server startup error: " + e.getMessage(), e);
        }
    }
}
