package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters.AudioFormatAdapter;
import com.google.gson.annotations.JsonAdapter;

import javax.sound.sampled.AudioFormat;
import java.util.Map;

public class CommandResponse {
    private final String status;
    private final String message;
    private final AccessKey accessKey;
    private final Map<Integer, Song> data;
    @JsonAdapter(AudioFormatAdapter.class)
    private final AudioFormat audioFormat;

    private CommandResponse(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.accessKey = builder.accessKey;
        this.data = builder.data;
        this.audioFormat = builder.audioFormat;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String message;
        private AccessKey accessKey;
        private Map<Integer, Song> data;
        private AudioFormat audioFormat;

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

        public CommandResponse build() {
            return new CommandResponse(this);
        }
    }
}