package bg.sofia.uni.fmi.mjt.spotify.server.songs;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SongFilenameParser {
    public static String[] parseFilename(Path filename) {
        Pattern pattern = Pattern.compile("^(.*) - (.*)\\.wav$");
        Matcher matcher = pattern.matcher(filename.toString());
        if (matcher.matches()) {
            String artist = matcher.group(1);
            String title = matcher.group(2);
            return new String[]{artist, title};
        }
        String title = filename.toString().replaceFirst("\\.wav$", "");
        return new String[]{"unknown", title};
    }
}