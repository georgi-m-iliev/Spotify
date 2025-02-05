package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.logging.Level;

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

    public UDPStreamServer(int port, InetAddress address, Path audioFilePath, AudioFormat audioFormat)  {
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
            System.out.println("UDP stream server working on port " + port);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFilePath.toFile());
            byte[] data = new byte[packetSize];
            int numBytesRead;

            long timing = (long) (600L * packetSize / audioFormat.getFrameRate() / audioFormat.getFrameSize());

            while ((numBytesRead = audioInputStream.read(data, 0, data.length)) != -1) {
                datagramPacket = new DatagramPacket(data, numBytesRead, address, port);
                socket.send(datagramPacket);
                Thread.sleep(timing); // Very basic form of flow control
            }

            // Send an empty packet to signal the end of the stream
            datagramPacket = new DatagramPacket(new byte[0], 0, address, port);
            socket.send(datagramPacket);
            System.out.println("UDP streaming finished.");
        } catch (SocketException e) {
            if (e.getMessage().equals("socket closed")) {
                SpotifyLogger.getLogger().log(
                        Level.INFO,
                        "UDP stream client on port closed");
            } else {
                SpotifyLogger.getLogger().log(
                        Level.WARNING,
                        "UDP stream server error: " + e.getMessage(),
                        e);
            }
        } catch (InterruptedException e) {
            SpotifyLogger.getLogger().log(
                    Level.INFO,
                    "UDP stream server terminated");
            Thread.currentThread().interrupt();
        } catch (IOException | UnsupportedAudioFileException e) {
            SpotifyLogger.getLogger().log(
                    Level.SEVERE,
                    "UDP stream server error: " + e.getMessage(),
                    e);
        }
    }
}