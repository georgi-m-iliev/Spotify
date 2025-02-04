package bg.sofia.uni.fmi.mjt.spotify.commons.exceptions;

public class SongLoadingFailureException extends Exception {
    public SongLoadingFailureException(String message) {
        super(message);
    }

    public SongLoadingFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
