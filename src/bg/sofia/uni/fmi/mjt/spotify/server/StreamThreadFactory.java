package bg.sofia.uni.fmi.mjt.spotify.server;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

public class StreamThreadFactory implements ThreadFactory {
    private final ConcurrentHashMap<SocketAddress, Thread> userThreads = new ConcurrentHashMap<>();

    @Override
    public Thread newThread(Runnable r) {
        SocketAddress socketAddress = getSocketAddressFromRunnable(r); // Implement this method to extract the user name from the Runnable
        if (userThreads.containsKey(socketAddress)) {
            return userThreads.get(socketAddress);
        }

        Thread thread = new Thread(r, "StreamThread" + socketAddress);
        userThreads.put(socketAddress, thread);
        return thread;
    }

    public void stopThread(SocketAddress socketAddress) {
        Thread thread = userThreads.get(socketAddress);
        if (thread != null) {
            thread.interrupt();
            userThreads.remove(socketAddress);
        }
    }

    private SocketAddress getSocketAddressFromRunnable(Runnable r) {
        return ((SocketAddressRunnable) r).getSocketAddress();
    }
}