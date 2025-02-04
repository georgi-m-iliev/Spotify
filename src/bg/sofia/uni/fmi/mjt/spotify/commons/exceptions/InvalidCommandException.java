package bg.sofia.uni.fmi.mjt.spotify.commons.exceptions;

public class InvalidCommandException extends Exception {
    public InvalidCommandException(String message) {
        super(message);
    }

    public InvalidCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
