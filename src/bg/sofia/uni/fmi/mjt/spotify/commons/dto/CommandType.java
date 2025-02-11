package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

public enum CommandType {
    REGISTER(2, false),
    LOGIN(2, false),
    SEARCH(1, false),
    TOP(1, false),
    CREATE_PLAYLIST(1, true),
    ADD_SONG_TO(2, true),
    SHOW_PLAYLIST(1, true),
    PLAY(2, true),
    STOP(0, true);

    private final int argumentsCount;
    private final boolean requiresAuthentication;

    CommandType(int argumentsCount, boolean requiresAuthentication) {
        this.argumentsCount = argumentsCount;
        this.requiresAuthentication = requiresAuthentication;
    }

    public int getArgumentsCount() {
        return argumentsCount;
    }

    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    public String getCommandName() {
        return this.name().toLowerCase().replace('_', '-');
    }

    public static CommandType fromString(String command) {
        return CommandType.valueOf(command.toUpperCase().replace('-', '_'));
    }
}
