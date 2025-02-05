package bg.sofia.uni.fmi.mjt.spotify.client;

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

public class TCPStreamClient implements Runnable {
    private final InetSocketAddress serverAddress;
    private final SourceDataLine sourceDataLine;
    private final AudioFormat audioFormat;

    public TCPStreamClient(InetAddress serverAddress, int port, AudioFormat audioFormat) throws LineUnavailableException {
        this.serverAddress = new InetSocketAddress(serverAddress, port);
        this.audioFormat = audioFormat;
        this.sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
        initSourceDataLine();
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
                // TODO: Log
            } else {
                // TODO: Log
            }
        }
    }

    private void closeSourceDataLine() {
        sourceDataLine.drain();
        sourceDataLine.close();
    }

    private void initSourceDataLine() throws LineUnavailableException {
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
    }
}
