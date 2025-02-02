package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import java.util.List;

public class CommandResponse {
    private final String status;
    private final String message;
    private final AccessKey accessKey;
    private final List<Song> data;

    private CommandResponse(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.accessKey = builder.accessKey;
        this.data = builder.data;
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

    public List<Song> data() {
        return data;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String message;
        private AccessKey accessKey;
        private List<Song> data;

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

        public Builder data(List<Song> data) {
            this.data = data;
            return this;
        }

        public CommandResponse build() {
            return new CommandResponse(this);
        }
    }
}