package bg.sofia.uni.fmi.mjt.spotify.server;

import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.FileInputStream;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;

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
            System.out.println("Server listening on port " + port);
            try (SocketChannel clientChannel = serverSocketChannel.accept();
                 FileInputStream fileInputStream = new FileInputStream(audioFilePath.toFile())) {
                System.out.println("Client connected: " + clientChannel.getRemoteAddress());

                ByteBuffer buffer = ByteBuffer.allocate(4096);
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer.array())) != -1) {
                    buffer.limit(bytesRead);
                    clientChannel.write(buffer);
                    buffer.clear();
                }
                System.out.println("File sent to client.");
            } catch (IOException e) {
                if (e.getMessage().equals("Connection reset by peer")) {
                    System.err.println("Client disconnected.");
                } else {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Error of starting server itself
            // TODO: Handle this error
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
