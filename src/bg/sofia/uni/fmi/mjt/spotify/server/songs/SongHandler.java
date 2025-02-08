package bg.sofia.uni.fmi.mjt.spotify.server.songs;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.SongLoadingFailureException;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

public class SongHandler implements AutoCloseable {
    private static final String HASH_ALGORITHM = "MD5";
    private static final Gson GSON = new Gson();

    private final List<Song> songs;
    private final Path songsDirPath;

    public SongHandler(Path songsDirPath) throws SongLoadingFailureException {
        this.songs = new ArrayList<>();
        this.songsDirPath = songsDirPath;
        loadSongs();
    }

    public void loadSongs() throws SongLoadingFailureException {
        if (!Files.exists(songsDirPath)) {
            throw new SongLoadingFailureException("Nonexistent path for songs directory");
        }
        if (!Files.isDirectory(songsDirPath)) {
            throw new SongLoadingFailureException("Invalid path for songs directory");
        }

        Path saveFilePath = Path.of(songsDirPath.toString(), "songs.json");
        List<Song> savedSongs = new ArrayList<>();
        if (Files.exists(saveFilePath)) {
            try (FileReader reader = new FileReader(saveFilePath.toFile())) {
                Song[] loadedSongs = GSON.fromJson(reader, Song[].class);
                if (loadedSongs != null) {
                    savedSongs.addAll(List.of(loadedSongs));
                }
            } catch (Exception e) {
                SpotifyLogger.warning("Failed to load songs from file", e);
            }
        }

        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(songsDirPath)) {
            for (Path songPath : dirStream) {
                File song = songPath.toFile();
                if (!song.getName().endsWith(".wav")) {
                    continue;
                }

                String[] songMetadata = SongFilenameParser.parseFilename(songPath.getFileName());
                try {
                    String songID = getHash(song, HASH_ALGORITHM);
                    Song savedSong = savedSongs.stream()
                            .filter(s -> s.id().equals(songID)).findFirst().orElse(null);
                    if (savedSong != null) {
                        songs.add(new Song(songID, songMetadata[0], songMetadata[1],
                                songPath, savedSong.streams()));
                    }
                    else {
                        songs.add(new Song(songID, songMetadata[0], songMetadata[1], songPath));
                    }
                } catch (NoSuchAlgorithmException | IOException e) {
                    SpotifyLogger.warning("Failed to load song: " + song.getName(), e);
                }
            }
        } catch (IOException e) {
            throw new SongLoadingFailureException("Failed to load songs", e);
        }

    }

    public void saveSongs() throws IOException {
        Path saveFilePath = Path.of(songsDirPath.toString(), "songs.json");
        if (saveFilePath.toFile().createNewFile()) {
            SpotifyLogger.info("Created new file for songs");
        }
        try (FileWriter writer = new FileWriter(saveFilePath.toFile())) {
            GSON.toJson(songs, writer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save songs", e);
        }
    }

    public static String getHash(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public List<Song> getSongs() {
        return Collections.unmodifiableList(songs);
    }

    public void flush() {
        try {
            saveSongs();
        } catch (IOException e) {
            SpotifyLogger.warning("Failed to save songs", e);
        }
    }

    @Override
    public void close() throws IOException {
        saveSongs();
    }
}
