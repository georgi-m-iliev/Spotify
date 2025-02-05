package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters.PlaylistAdapter;

import java.util.List;
import java.util.UUID;

public record Playlist(UUID id, String name, List<Song> songs) {
    public Playlist(String name, List<Song> songs) {
        this(UUID.randomUUID(), name, songs);
    }

    public static Playlist of(String name) {
        return new Playlist(UUID.randomUUID(), name, List.of());
    }
}
