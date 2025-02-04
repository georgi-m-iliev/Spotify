package bg.sofia.uni.fmi.mjt.spotify.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class TCPStreamClient implements Runnable {
    private final InetSocketAddress serverAddress;
    private final SourceDataLine dataLine;
    private final AudioFormat audioFormat;

    public TCPStreamClient(InetAddress serverAddress, int port, AudioFormat audioFormat) throws LineUnavailableException {
        this.serverAddress = new InetSocketAddress(serverAddress, port);
        this.audioFormat = audioFormat;
        this.dataLine = AudioSystem.getSourceDataLine(audioFormat);
    }

    @Override
    public void run() {
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            dataLine.open(audioFormat);
            dataLine.start();

            while (socketChannel.read(buffer) != -1) {
                buffer.flip();
                byte[] audioData = new byte[buffer.remaining()];
                buffer.get(audioData);
                int frameSize = audioFormat.getFrameSize();
                int bytesToWrite = (audioData.length / frameSize) * frameSize;
                dataLine.write(audioData, 0, bytesToWrite);
                buffer.clear();
            }
            dataLine.drain();
            dataLine.close();
        } catch (IOException e) {
            if (e instanceof SocketException && e.getMessage().equals("Connection reset")) {
                System.err.println("Server closed the connection.");
            } else {
                System.err.println("Error streaming audio: " + e.getMessage());
            }
        } catch (LineUnavailableException e) {
            System.err.println("Error opening audio line: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        // Example usage
        InetAddress serverAddress = InetAddress.getByName("localhost");
        AudioFormat audioFormat = AudioSystem.getAudioFileFormat(new File("resources/testк.wav")).getFormat();
        TCPStreamClient client = new TCPStreamClient(serverAddress, 7777, audioFormat);
        Thread clientThread = new Thread(client);
        clientThread.start();
    }
}
