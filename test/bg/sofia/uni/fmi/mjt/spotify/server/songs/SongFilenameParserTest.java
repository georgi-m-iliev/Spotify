package bg.sofia.uni.fmi.mjt.spotify.server.songs;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SongFilenameParserTest {
    @Test
    void parseFilenameFormatted() {
        String artist = "artist";
        String title = "title";
        Path filename = Path.of(String.format("%s - %s.wav", artist, title));
        String[] result = SongFilenameParser.parseFilename(filename);
        assertEquals(artist, result[0]);
        assertEquals(title, result[1]);
    }

    @Test
    void parseFilenameUnformatted() {
        String title = "filename";
        Path filename = Path.of(String.format("%s.wav", title));
        String[] result = SongFilenameParser.parseFilename(filename);
        assertEquals("unknown", result[0]);
        assertEquals(title, result[1]);
    }
}