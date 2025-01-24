package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;

public class SpotifyServerTest {
    public static void main(String[] args) {
        int port = 5000; // Server port
        String filePath = "resources/toshka_cropped.wav"; // Path to the audio file
        
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new java.net.InetSocketAddress(port));
            System.out.println("Server listening on port " + port);

            while (true) {
                try (SocketChannel clientChannel = serverSocketChannel.accept();
                     FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
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
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
