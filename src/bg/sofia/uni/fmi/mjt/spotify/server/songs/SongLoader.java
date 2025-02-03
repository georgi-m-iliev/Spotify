package bg.sofia.uni.fmi.mjt.spotify.server.songs;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.server.exceptions.SongLoadingFailureException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SongLoader {

    public static List<Song> loadSongs(Path songsDirPath) throws SongLoadingFailureException {
        if (!Files.exists(songsDirPath)) {
            throw new SongLoadingFailureException("Nonexistent path for songs directory");
        }
        if (!Files.isDirectory(songsDirPath)) {
            throw new SongLoadingFailureException("Invalid path for songs directory");
        }

        List<Song> availableSongs = new ArrayList<>();
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(songsDirPath)) {
            int ind = 0;
            for (Path songPath : dirStream) {
                if (!songPath.toFile().getName().endsWith(".wav")) {
                    continue;
                }

                String[] songMetadata = SongFilenameParser.parseFilename(songPath.getFileName());

                availableSongs.add(new Song(ind++, songMetadata[0], songMetadata[1], songPath));
            }
        } catch (IOException e) {
            throw new SongLoadingFailureException("Failed to load songs", e);
        }

        return availableSongs;
    }
}
