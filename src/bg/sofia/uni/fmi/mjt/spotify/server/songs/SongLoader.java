package bg.sofia.uni.fmi.mjt.spotify.server.songs;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.server.exceptions.SongLoadingFailureException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
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
            for (Path songPath : dirStream) {
                File song = songPath.toFile();
                if (!song.getName().endsWith(".wav")) {
                    continue;
                }

                String[] songMetadata = SongFilenameParser.parseFilename(songPath.getFileName());
                try {
                    String songID = getHash(song, "MD5");
                    availableSongs.add(new Song(songID, songMetadata[0], songMetadata[1], songPath));
                } catch (NoSuchAlgorithmException | IOException e) {
                    // TODO: Log song read error
                }
            }
        } catch (IOException e) {
            throw new SongLoadingFailureException("Failed to load songs", e);
        }

        return availableSongs;
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
}
