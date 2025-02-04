package bg.sofia.uni.fmi.mjt.spotify.commons;

import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.NoFreePortAvailableException;

import java.io.IOException;
import java.net.ServerSocket;

public class NetworkTools {
    public static int findFreePort() throws NoFreePortAvailableException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new NoFreePortAvailableException("No free port available", e);
        }
    }
}
