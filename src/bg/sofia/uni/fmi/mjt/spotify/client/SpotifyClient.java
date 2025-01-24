package bg.sofia.uni.fmi.mjt.spotify.client;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SpotifyClient {
    public static void main(String[] args) {
        String serverHost = "localhost"; // Server address
        int port = 5000; // Server port
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, port);
        
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            System.out.println("Connected to server.");

            ByteBuffer buffer = ByteBuffer.allocate(4096);
            AudioFormat audioFormat = AudioSystem.getAudioInputStream(new File("resources/toshka.wav")).getFormat();
            SourceDataLine dataLine = AudioSystem.getSourceDataLine(audioFormat);
            dataLine.open(audioFormat);
            dataLine.start();

            while (socketChannel.read(buffer) != -1) {
                buffer.flip();

                // Write audio data to SourceDataLine
                byte[] audioData = new byte[buffer.remaining()];
                buffer.get(audioData);
                int frameSize = audioFormat.getFrameSize();
                int bytesToWrite = (audioData.length / frameSize) * frameSize;
                dataLine.write(audioData, 0, bytesToWrite);
                buffer.clear();
            }

            // Drain and close the line after playback
            dataLine.drain();
            dataLine.close();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
