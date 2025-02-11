package bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaylistAdapterTest {
    @Test
    void testSerialize() {
        List<Song> songs = List.of(
                new Song("1", "song1", "artist1", Path.of("song1.wav"), 200),
                new Song("2", "song2", "artist2", Path.of("song2.wav"), 300)
        );

        Playlist playlist = Playlist.of("playlist1");
        playlist.songs().addAll(songs);
        String playlistUUID = playlist.id().toString();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Playlist.class, new PlaylistAdapter(songs))
                .create();

        String expected = String.format("""
                {
                    "id": "%s",
                    "name": "playlist1",
                    "songs": [
                        "1",
                        "2"
                    ]
                }
                """, playlistUUID);
        String actual = gson.toJson(playlist);
        assertEquals(expected.replaceAll("\\s", ""), actual);

    }

    @Test
    void testDeserialize() {
        String json = String.format("""
        {
            "id": "%s",
            "name": "playlist1",
            "songs": [
                "1",
                "2",
                "3"
            ]
        }
        """, UUID.randomUUID());

        List<Song> songs = List.of(
            new Song("1", "song1", "artist1", Path.of("song1.wav"), 200),
            new Song("2", "song2", "artist2", Path.of("song2.wav"), 300)
        );

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Playlist.class, new PlaylistAdapter(songs))
            .create();

        Playlist playlist = gson.fromJson(json, Playlist.class);
        assertEquals("playlist1", playlist.name());
        assertEquals(2, playlist.songs().size());
        assertEquals("1", playlist.songs().get(0).id());
        assertEquals("2", playlist.songs().get(1).id());
    }
}