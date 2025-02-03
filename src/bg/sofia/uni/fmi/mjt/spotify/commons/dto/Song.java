package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import java.nio.file.Path;
import java.util.Objects;

public class Song {
    private final int index;
    private final String artist;
    private final String name;
    private final transient Path path;

    public Song(int index, String artist, String name, Path path) {
        this.index = index;
        this.name = name;
        this.artist = artist;
        this.path = path;
    }

    public int index() {
        return index;
    }

    public String artist() {
        return artist;
    }

    public String name() {
        return name;
    }

    public Path path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Song song)) return false;
        return Objects.equals(name, song.name) && Objects.equals(artist, song.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, artist);
    }
}
