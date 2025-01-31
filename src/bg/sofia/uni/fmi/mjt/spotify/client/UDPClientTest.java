package bg.sofia.uni.fmi.mjt.spotify.client;

import java.io.File;
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

    static DataLine.Info dataLineInfo;
    static SourceDataLine sourceDataLine;

    private static final int BUFFER_SIZE = 1000; // Number of packets to buffer
    private static final BlockingQueue<byte[]> bufferQueue = new LinkedBlockingQueue<>(BUFFER_SIZE);

    public static void main(String[] args) throws Exception {
        System.out.println("Server started at port:" + port);

        DatagramSocket serverSocket = new DatagramSocket(port);

        byte[] receiveData = new byte[9000];

        format = AudioSystem.getAudioFileFormat(new File("resources/toshka.wav")).getFormat();
        dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(format);
        sourceDataLine.start();

        Thread playbackThread = new Thread(() -> {
            try {
                while (true) {
                    if (bufferQueue.isEmpty()) {
                        if (status == false) {
                            break;
                        }
                        continue;
                    }
                    byte[] audioData = bufferQueue.take();
                    toSpeaker(audioData);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        playbackThread.start();

        long start = 0;
        long end = 0;
        while(status == true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            if (start == 0) {
                start = System.currentTimeMillis();
            }
            // if the packet is empty, it means the stream is finished
            if (receivePacket.getLength() == 0) {
                status = false;
                break;
            }
            byte[] audioData = new byte[receivePacket.getLength()];
            System.arraycopy(receivePacket.getData(), 0, audioData, 0, receivePacket.getLength());
            bufferQueue.put(audioData);
        }

        playbackThread.join();
        end = System.currentTimeMillis();

        System.out.println("Time taken to play the audio: " + (end - start) + "ms");
        System.out.println("in seconds: " + (end - start) / 1000 + "s");

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