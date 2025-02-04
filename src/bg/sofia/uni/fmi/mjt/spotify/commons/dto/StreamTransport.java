package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

public enum StreamTransport {
    TCP, UDP;

    public static StreamTransport of(String arg) {
        return StreamTransport.valueOf(arg.toUpperCase());
    }
}
