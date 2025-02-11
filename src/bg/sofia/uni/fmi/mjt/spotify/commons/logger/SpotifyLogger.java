package bg.sofia.uni.fmi.mjt.spotify.commons.logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;


public class SpotifyLogger {
    private static final String BASE_PACKAGE = "bg.sofia.uni.fmi.mjt.spotify.";
    private static final Logger logger = Logger.getLogger("SpotifyLogger");

    static {
        try {
            LogManager.getLogManager().reset();
            logger.setLevel(Level.ALL);

            FileHandler fileHandler = new FileHandler("app.log", true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(
                new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                        String side = "";
                        if (record.getSourceClassName().contains("server")) {
                            side = "Server";
                        } else if (record.getSourceClassName().contains("client")) {
                            side = "Client";
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append(String.format("[%s] [%s] [%s] %s - %s%n",
                            dateFormat.format(new Date(record.getMillis())),
                            side,
                            record.getLevel(),
                            record.getSourceClassName().replace(BASE_PACKAGE, ""),
                            record.getMessage()));
                        if (record.getThrown() != null) {
                            sb.append("Exception: ").append(record.getThrown().toString()).append("\n");
                            for (StackTraceElement element : record.getThrown().getStackTrace()) {
                                sb.append("\tat ").append(element.toString()).append("\n");
                            }
                        }
                        return sb.toString();
                    }
                });
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    private SpotifyLogger() {}

    public static Logger logger() {
        return logger;
    }
}
