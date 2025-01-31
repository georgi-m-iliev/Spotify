package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import java.io.Serializable;

public record AccessKey(String username, String token) implements Serializable {
    public static AccessKey of(String username, String token) {
        return new AccessKey(username, token);
    }
}
