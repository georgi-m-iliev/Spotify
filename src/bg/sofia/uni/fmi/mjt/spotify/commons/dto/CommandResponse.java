package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters.AudioFormatAdapter;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;
import com.google.gson.annotations.JsonAdapter;

import javax.sound.sampled.AudioFormat;
import java.util.Map;
import java.util.logging.Level;

public class CommandResponse {
    private final String status;
    private final String message;
    private final AccessKey accessKey;
    private final Map<Integer, Song> data;
    @JsonAdapter(AudioFormatAdapter.class)
    private final AudioFormat audioFormat;
    private final StreamTransport transport;

    private CommandResponse(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.accessKey = builder.accessKey;
        this.data = builder.data;
        this.audioFormat = builder.audioFormat;
        this.transport = builder.transport;
    }

    public String status() {
        return status;
    }

    public String message() {
        return message;
    }

    public AccessKey accessKey() {
        return accessKey;
    }

    public Map<Integer, Song> data() {
        return data;
    }

    public AudioFormat audioFormat() {
        return audioFormat;
    }

    public StreamTransport transport() {
        return transport;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String message;
        private AccessKey accessKey;
        private Map<Integer, Song> data;
        private AudioFormat audioFormat;
        private StreamTransport transport;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder accessKey(AccessKey accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public Builder data(Map<Integer, Song> data) {
            this.data = data;
            return this;
        }

        public Builder audioFormat(AudioFormat audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }

        public Builder transport(StreamTransport transport) {
            this.transport = transport;
            return this;
        }

        public CommandResponse build() {
            return new CommandResponse(this);
        }

        public CommandResponse buildError(String message) {
            SpotifyLogger.getLogger().log(
                Level.FINE,
                String.format("User %s command failed with message %s",
                        accessKey != null ? accessKey.username() : "ANON",
                        message));
            return CommandResponse.builder().status("ERROR").message(message).build();
        }

        public CommandResponse buildOK(String message) {
            SpotifyLogger.getLogger().log(
                Level.FINER,
                String.format("User %s command succeeded with message %s",
                        accessKey != null ? accessKey.username() : "ANON",
                        message));
            return CommandResponse.builder().status("OK").message(message).build();
        }
    }
}