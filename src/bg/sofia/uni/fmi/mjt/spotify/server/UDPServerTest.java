package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class UDPServerTest {
    public static void main(String[] args) {
        DatagramPacket datagramPacket;
        InetAddress addr;
        int port = 50005;
        int bufferSize = 9000; // Adjusted buffer size

        try(DatagramSocket socket = new DatagramSocket()) {
            addr = InetAddress.getByName("127.0.0.1");
            File audioFile = new File("resources/toshka_cropped.wav");
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();

            byte[] data = new byte[bufferSize];
            int numBytesRead;

            long timing = (long) (800 * bufferSize / format.getFrameRate() / format.getFrameSize());

            while ((numBytesRead = audioInputStream.read(data, 0, data.length)) != -1) {
                datagramPacket = new DatagramPacket(data, numBytesRead, addr, port);
                socket.send(datagramPacket);
                Thread.sleep(timing); // Adjusting the sleep time
            }

            // Send an empty packet to signal the end of the stream
            datagramPacket = new DatagramPacket(new byte[0], 0, addr, port);
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
}