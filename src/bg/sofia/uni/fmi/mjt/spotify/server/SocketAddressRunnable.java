package bg.sofia.uni.fmi.mjt.spotify.server;

import java.net.SocketAddress;

public interface SocketAddressRunnable extends Runnable {
    public SocketAddress getSocketAddress();
}
