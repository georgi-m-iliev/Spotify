package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.StreamTransport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyClientTest {

    @Test
    void testDisconnect() throws IOException {
        SocketChannel socketChannel = Mockito.mock(SocketChannel.class);
        Mockito.when(socketChannel.isConnected()).thenReturn(true);
        Socket socket = Mockito.mock(Socket.class);
        Mockito.when(socketChannel.socket()).thenReturn(socket);
        Mockito.when(socketChannel.getRemoteAddress()).thenReturn(InetSocketAddress.createUnresolved("localhost", 8080));

        SpotifyClient client = new SpotifyClient(socketChannel, StreamTransport.UDP);

        String simulatedInput = "disconnect\r\n";
        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

        try {
            client.enter();
        } catch (Exception e) {
            fail("Exception thrown");
        } finally {
            System.setIn(originalIn);
        }

        Mockito.verify(socketChannel).close();
    }
}