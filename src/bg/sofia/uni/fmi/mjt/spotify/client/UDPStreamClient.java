package bg.sofia.uni.fmi.mjt.spotify.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class UDPStreamClient implements Runnable {
    private static final int DEFAULT_BUFFER_SIZE = 5000; // Number of packets to buffer
    private static final int DEFAULT_PACKET_SIZE = 9000;

    private final int port;
    private final int packetSize;
    private final AudioFormat audioFormat;
    private final AtomicBoolean status;

    private final SourceDataLine sourceDataLine;
    private final BlockingQueue<byte[]> bufferQueue;

    public UDPStreamClient(int port, AudioFormat audioFormat) throws LineUnavailableException {
        this.port = port;
        this.packetSize = DEFAULT_PACKET_SIZE;
        this.audioFormat = audioFormat;
        this.status = new AtomicBoolean(true);
        this.sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
        bufferQueue = new LinkedBlockingQueue<>(DEFAULT_BUFFER_SIZE);
        initSourceDataLine();
    }

    @Override
    public void run() {
        Thread playbackThread = null;
        try(DatagramSocket serverSocket = new DatagramSocket(port)) {
            byte[] packetData = new byte[packetSize];
            playbackThread = getPlayer();

            while (status.get()) {
                DatagramPacket receivePacket = new DatagramPacket(packetData, packetData.length);
                serverSocket.receive(receivePacket);
                // if the packet is empty, it means the stream is finished
                if (receivePacket.getLength() == 0) {
                    // TODO: Log that the stream is finished
                    status.set(false);
                    break;
                }
                packetToAudioStream(receivePacket);
            }

            playbackThread.join();
            closeSourceDataLine();
        } catch (IOException e) {
            // TODO: Log
        } catch (InterruptedException e) {
            playbackThread.interrupt();
            sourceDataLine.flush();
            sourceDataLine.close();
        }
    }

    private void packetToAudioStream(DatagramPacket receivePacket) throws InterruptedException {
        byte[] audioData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
        bufferQueue.put(audioData);
    }

    private void closeSourceDataLine() {
        sourceDataLine.drain();
        sourceDataLine.close();
    }

    private void initSourceDataLine() throws LineUnavailableException {
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();
    }

    private Thread getPlayer() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    if (bufferQueue.isEmpty()) {
                        if (!status.get()) {
                            System.out.println("Playback finished.");
                            break;
                        }
                        continue;
                    }
                    byte[] audioData = bufferQueue.take();
                    sourceDataLine.write(audioData, 0, audioData.length);

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        return thread;
    }
}