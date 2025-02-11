package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.PlaybackFailedException;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineUnavailableException;

public class TCPStreamClient implements Runnable {
    private final InetSocketAddress serverAddress;
    private final AudioFormat audioFormat;
    private SourceDataLine sourceDataLine;

    public TCPStreamClient(InetAddress serverAddress, int port, AudioFormat audioFormat) throws PlaybackFailedException {
        this.serverAddress = new InetSocketAddress(serverAddress, port);
        this.audioFormat = audioFormat;
        try {
            initSourceDataLine();
        } catch (LineUnavailableException e) {
            SpotifyLogger.logger().log(Level.SEVERE, "Failed to initialize audio playback", e);
            throw new PlaybackFailedException("Failed to initialize audio playback", e);
        }
    }

    @Override
    public void run() {
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (socketChannel.read(buffer) != -1) {
                buffer.flip();
                byte[] audioData = new byte[buffer.remaining()];
                buffer.get(audioData);
                int frameSize = audioFormat.getFrameSize();
                int bytesToWrite = (audioData.length / frameSize) * frameSize;
                sourceDataLine.write(audioData, 0, bytesToWrite);
                buffer.clear();
            }
            closeSourceDataLine();
        } catch (IOException e) {
            if (e instanceof SocketException && e.getMessage().equals("Connection reset")) {
                SpotifyLogger.logger().warning("The server has closed the streaming connection");
            } else {
                SpotifyLogger.logger().log(Level.SEVERE, "An error occurred while streaming audio", e);
            }
        }
    }

    private void closeSourceDataLine() {
        sourceDataLine.drain();
        sourceDataLine.close();
    }

    private void initSourceDataLine() throws LineUnavailableException {
        sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
    }
}
