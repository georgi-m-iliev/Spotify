package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class UDPStreamServer implements Runnable {
    private final static int DEFAULT_PACKET_SIZE = 9000;

    private final int port;
    private final int packetSize;
    private final InetAddress address;
    private final Path audioFilePath;
    private final AudioFormat audioFormat;

    public UDPStreamServer(int port, InetAddress address, Path audioFilePath, AudioFormat audioFormat) {
        this.port = port;
        this.packetSize = DEFAULT_PACKET_SIZE;
        this.address = address;
        this.audioFilePath = audioFilePath;
        this.audioFormat = audioFormat;
    }

    @Override
    public void run() {
        DatagramPacket datagramPacket;
        try(DatagramSocket socket = new DatagramSocket()) {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFilePath.toFile());
            byte[] data = new byte[packetSize];
            int numBytesRead;

            long timing = (long) (200L * packetSize / audioFormat.getFrameRate() / audioFormat.getFrameSize());

            while ((numBytesRead = audioInputStream.read(data, 0, data.length)) != -1) {
                datagramPacket = new DatagramPacket(data, numBytesRead, address, port);
                socket.send(datagramPacket);
                Thread.sleep(timing); // Very basic form of flow control
            }

            // Send an empty packet to signal the end of the stream
            datagramPacket = new DatagramPacket(new byte[0], 0, address, port);
            socket.send(datagramPacket);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Example usage
        try {
            AudioFormat audioFormat = AudioSystem.getAudioFileFormat(Path.of("resources/Jluch - Bulgarskite Seriali.wav").toFile()).getFormat();
            new Thread(new UDPStreamServer(7777, InetAddress.getByName("localhost"), Path.of("resources/Jluch - Bulgarskite Seriali.wav"), audioFormat)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}