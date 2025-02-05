package bg.sofia.uni.fmi.mjt.spotify.client;

public enum ClientCommandType {
    CONNECT,
    DISCONNECT,
    REGISTER,
    LOGIN,
    LOGOUT,
    SEARCH,
    TOP,
    CREATE_PLAYLIST,
    ADD_SONG_TO,
    SHOW_PLAYLIST,
    PLAY,
    STOP,
    EXIT;

    public String getCommandName() {
        return this.name().toLowerCase().replace('_', '-');
    }

    public static ClientCommandType getCommandType(String command) {
        return ClientCommandType.valueOf(command.toUpperCase().replace('-', '_'));
    }
}
