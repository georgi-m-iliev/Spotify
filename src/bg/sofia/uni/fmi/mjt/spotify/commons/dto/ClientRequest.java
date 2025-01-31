package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

public record ClientRequest(String command, AccessKey accessKey) {
    public static ClientRequest of(String command, AccessKey accessKey) {
        return new ClientRequest(command, accessKey);
    }
}
