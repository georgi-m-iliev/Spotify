package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import java.util.List;

public record Playlist(String name, List<Song> songs) {
    public static Playlist of(String name) {
        return new Playlist(name, List.of());
    }
}
