package bg.sofia.uni.fmi.mjt.spotify.commons.exceptions;

public class PlaybackFailedException extends Exception {
    public PlaybackFailedException(String message) {
        super(message);
    }

    public PlaybackFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
