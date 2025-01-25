package bg.sofia.uni.fmi.mjt.spotify.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class UDPClientTest {

    static AudioFormat format;
    static boolean status = true;
    static int port = 50005;
    static int sampleRate = 48000;

    static DataLine.Info dataLineInfo;
    static SourceDataLine sourceDataLine;

    private static final int BUFFER_SIZE = 10; // Number of packets to buffer
    private static final BlockingQueue<byte[]> bufferQueue = new LinkedBlockingQueue<>(BUFFER_SIZE);

    public static void main(String[] args) throws Exception {
        System.out.println("Server started at port:" + port);

        DatagramSocket serverSocket = new DatagramSocket(port);

        byte[] receiveData = new byte[9000];

        format = new AudioFormat(sampleRate, 16, 2, true, false);
        dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(format);
        sourceDataLine.start();

        Thread playbackThread = new Thread(() -> {
            try {
                while (status) {
                    byte[] audioData = bufferQueue.take();
                    toSpeaker(audioData);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        playbackThread.start();

        while (status) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            byte[] audioData = new byte[receivePacket.getLength()];
            System.arraycopy(receivePacket.getData(), 0, audioData, 0, receivePacket.getLength());
            bufferQueue.put(audioData);
        }

        sourceDataLine.drain();
        sourceDataLine.close();
        serverSocket.close();
    }

    public static void toSpeaker(byte[] soundbytes) {
        try {
            sourceDataLine.write(soundbytes, 0, soundbytes.length);
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }
}