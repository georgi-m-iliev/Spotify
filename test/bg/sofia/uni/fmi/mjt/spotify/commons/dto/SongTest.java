package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SongTest {
    @Test
    void testEquals() {
        Song song1 = new Song("id1", "song1", "artist1", Path.of("song1.wav"));
        Song song2 = new Song("id1", "song1", "artist1", Path.of("song2.wav"));
        assertEquals(song1, song2);
    }

    @Test
    void testNotEquals() {
        Song song1 = new Song("id1", "song1", "artist1", Path.of("song1.wav"));
        Song song3 = new Song("id2", "song2", "artist2", Path.of("song3.wav"));
        assertNotEquals(song1, song3);
    }

    @Test
    void testEqualsNotObject() {
        Song song1 = new Song("id1", "song1", "artist1", Path.of("song1.wav"));
        assertNotEquals(new Object(), song1);
    }

    @Test
    void testHashCode() {
        Song song1 = new Song("id1", "song1", "artist1", Path.of("song1.wav"));
        Song song2 = new Song("id1", "song1", "artist1", Path.of("song2.wav"));
        assertEquals(song1.hashCode(), song2.hashCode());
    }
}