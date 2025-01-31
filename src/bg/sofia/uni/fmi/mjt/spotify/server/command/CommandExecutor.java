package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.server.users.User;
import bg.sofia.uni.fmi.mjt.spotify.server.users.UserStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandExecutor {
    private final UserStorage users;
    private final List<Song> songs;
    private final Set<User> loggedInUsers;

    public CommandExecutor(UserStorage users, List<Song> songs) {
        if (users == null || songs == null) {
            throw new IllegalArgumentException("Users and songs cannot be null.");
        }
        this.users = users;
        this.songs = songs;
        this.loggedInUsers = new HashSet<>();
    }

    public CommandResponse execute(Command cmd) {
        return switch (cmd.command()) {
            case REGISTER -> register(cmd.arguments());
            case LOGIN -> login(cmd.arguments());
            case LOGOUT -> logout(cmd.arguments(), cmd.accessKey());
            case DISCONNECT -> null;
            case SEARCH -> search(cmd.arguments(), cmd.accessKey());
            case TOP -> top(cmd.arguments(), cmd.accessKey());
            case CREATE_PLAYLIST -> createPlaylist(cmd.arguments(), cmd.accessKey());
            case ADD_SONG_TO -> addSongTo(cmd.arguments(), cmd.accessKey());
            case SHOW_PLAYLIST -> showPlaylist(cmd.arguments(), cmd.accessKey());
            case PLAY -> play(cmd.arguments(), cmd.accessKey());
        };
    }

    private CommandResponse register(String[] args) {
        if (args.length != CommandType.REGISTER.getArgumentsCount()) {
            return CommandResponse.of("OK", "register expects 2 arguments.");
        }

        String email = args[0];
        String password = args[1];

        users.addUser(User.of(email, password));

        return CommandResponse.of("OK", "User registered successfully.");
    }

    private CommandResponse login(String[] args) {
        if (args.length != CommandType.LOGIN.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "login expects 2 arguments.");
        }

        String email = args[0];
        String password = args[1];
        User user = users.getUser(email);
        if (user == null) {
            return CommandResponse.of("ERROR", "Invalid email, user not found.");
        }

        if (loggedInUsers.contains(user)) {
            return CommandResponse.of("ERROR", "User is already logged in.");
        }

        if (user.validatePassword(password)) {
            loggedInUsers.add(user);
            return CommandResponse.of("OK", "User logged in successfully.", user.getAccessKey());
        }
        return CommandResponse.of("ERROR", "Invalid email/password.");
    }

    private CommandResponse logout(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length != CommandType.LOGOUT.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "logout expects 0 arguments.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        if (!loggedInUsers.contains(user)) {
            return CommandResponse.of("ERROR", "User is not logged in.");
        }

        loggedInUsers.remove(user);
        return CommandResponse.of("OK", "User logged out successfully.");
    }

    private CommandResponse search(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length < CommandType.SEARCH.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "search expects at least 1 argument.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        List<Song> results = songs.stream()
                .filter(song -> {
                    for (String keyword : args) {
                        if (song.name().toLowerCase().contains(keyword.toLowerCase()) ||
                            song.artist().toLowerCase().contains(keyword.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
        return CommandResponse.of("OK", String.format("Search has returned %s results", results.size()), results);
    }

    private CommandResponse createPlaylist(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length != CommandType.CREATE_PLAYLIST.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "create-playlist expects 1 argument.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        if (user.getPlaylists().stream().anyMatch(playlist -> playlist.name().equals(args[0]))) {
            return CommandResponse.of("ERROR", "Playlist with the same name already exists.");
        }

        user.getPlaylists().add(Playlist.of(args[0]));
        return CommandResponse.of("OK", String.format("Playlist %s created successfully.", args[0]));
    }

    private CommandResponse addSongTo(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length != CommandType.ADD_SONG_TO.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "add-song-to expects 2 argument.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        Playlist playlist = user.getPlaylists().stream()
                .filter(p -> p.name().equals(args[0]))
                .findFirst()
                .orElse(null);
        if (playlist == null) {
            return CommandResponse.of("ERROR", "Playlist with this name doesn't exists.");
        }

        int songIndex;
        try {
            songIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return CommandResponse.of("ERROR", "Invalid song index.");
        }

        Song toAdd = songs.stream()
                .filter(s -> s.index() == songIndex)
                .findFirst()
                .orElse(null);
        if (toAdd == null) {
            return CommandResponse.of("ERROR", "Song with this index doesn't exists or is already added.");
        }

        if (playlist.songs().contains(toAdd)) {
            return CommandResponse.of("ERROR", "Song is already added to the playlist.");
        }

        playlist.songs().add(toAdd);
        return CommandResponse.of("OK", "Song successfully added to playlist.");
    }

    private CommandResponse showPlaylist(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length != CommandType.SHOW_PLAYLIST.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "show-playlist expects 1 argument.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        Playlist playlist = user.getPlaylists().stream()
                .filter(p -> p.name().equals(args[0]))
                .findFirst()
                .orElse(null);
        if (playlist == null) {
            return CommandResponse.of("ERROR", "Playlist with this name doesn't exists.");
        }

        return CommandResponse.of(
            "OK",
            String.format("Playlist %s contains %s song(s).", playlist.name(), playlist.songs().size()),
            playlist.songs());
    }

    public CommandResponse play(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length != CommandType.PLAY.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "play expects 1 argument.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        int songIndex;
        try {
            songIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.of("ERROR", "Invalid song index.");
        }

        Song song = songs.stream()
                .filter(s -> s.index() == songIndex)
                .findFirst()
                .orElse(null);
        if (song == null) {
            return CommandResponse.of("ERROR", "Song with this index doesn't exists.");
        }

        // TODO: Implement playing a song

        return CommandResponse.of("OK", String.format("Playing %s by %s", song.name(), song.artist()));
    }

    public CommandResponse top(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.of("ERROR", "You are not logged in.");
        }
        if (args.length != CommandType.TOP.getArgumentsCount()) {
            return CommandResponse.of("ERROR", "top expects 1 argument.");
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.of("ERROR", "Invalid access key.");
        }

        int topCount;
        try {
            topCount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.of("ERROR", "Invalid top count.");
        }

        // TODO: Implement top mechanic

        return CommandResponse.of("OK", "Message", List.of());
    }

}