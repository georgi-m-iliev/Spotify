package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandType;

public record Command(CommandType command, String[] arguments, AccessKey accessKey) {
    public Command(CommandType command, String[] arguments) {
        this(command, arguments, null);
    }
}