package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import java.util.List;

public record   CommandResponse(String status, String message, AccessKey accessKey, List<Song> data) {
    public CommandResponse(String status, String message) {
        this(status, message, null, null);
    }

    public static CommandResponse of(String status, String message) {
        return new CommandResponse(status, message);
    }

    public static CommandResponse of(String status, String message, AccessKey accessKey) {
        return new CommandResponse(status, message, accessKey, null);
    }

    public static CommandResponse of(String status, String message, List<Song> data) {
        return new CommandResponse(status, message, null, data);
    }
}
