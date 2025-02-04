package bg.sofia.uni.fmi.mjt.spotify.commons.exceptions;

public class NoFreePortAvailableException extends Exception {
    public NoFreePortAvailableException(String message) {
        super(message);
    }

    public NoFreePortAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
