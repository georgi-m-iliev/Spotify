package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters.AudioFormatAdapter;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;
import com.google.gson.annotations.JsonAdapter;

import javax.sound.sampled.AudioFormat;
import java.util.Map;

public record CommandResponse (
    String status,
    String message,
    AccessKey accessKey,
    Map<Integer, Song> data,
    @JsonAdapter(AudioFormatAdapter.class)
    AudioFormat audioFormat,
    StreamTransport transport) {

    private CommandResponse(Builder builder) {
        this(builder.status, builder.message, builder.accessKey,
                builder.data, builder.audioFormat, builder.transport);
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
            SpotifyLogger.logger().fine(String.format("User command failed with message %s", message));
            return CommandResponse.builder().status("ERROR").message(message).build();
        }

        public CommandResponse buildOK(String message) {
            SpotifyLogger.logger().fine(String.format("User command succeeded with message %s", message));
            return CommandResponse.builder().status("OK").message(message).build();
        }
    }
}