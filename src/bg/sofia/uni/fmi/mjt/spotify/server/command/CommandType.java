package bg.sofia.uni.fmi.mjt.spotify.server.command;

public enum CommandType {
    REGISTER(2),
    LOGIN(2),
    LOGOUT(0),
    DISCONNECT(0),
    SEARCH(-1),
    TOP(1),
    CREATE_PLAYLIST(1),
    ADD_SONG_TO(2),
    SHOW_PLAYLIST(1),
    PLAY(1);

    private final int argumentsCount;

    CommandType(int argumentsCount) {
        this.argumentsCount = argumentsCount;
    }

    public int getArgumentsCount() {
        return argumentsCount;
    }

    public String getCommandName() {
        return this.name().toLowerCase().replace('_', '-');
    }

    public static CommandType fromString(String command) {
        return CommandType.valueOf(command.toUpperCase().replace('-', '_'));
    }
}
