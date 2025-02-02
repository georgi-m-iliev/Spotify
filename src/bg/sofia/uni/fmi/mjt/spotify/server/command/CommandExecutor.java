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
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("register expects 2 arguments.")
                    .build();
        }

        String email = args[0];
        String password = args[1];

        users.addUser(User.of(email, password));

        return CommandResponse.builder()
                .status("OK")
                .message("User registered successfully.")
                .build();
    }

    private CommandResponse login(String[] args) {
        if (args.length != CommandType.LOGIN.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("login expects 2 arguments.")
                    .build();
        }

        String email = args[0];
        String password = args[1];
        User user = users.getUser(email);
        if (user == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("User with this email was not found.")
                    .build();
        }

        if (loggedInUsers.contains(user)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("User is already logged in.")
                    .build();
        }

        if (user.validatePassword(password)) {
            loggedInUsers.add(user);
            return CommandResponse.builder()
                    .status("OK")
                    .message("User logged in successfully.")
                    .accessKey(user.getAccessKey())
                    .build();
        }
        return CommandResponse.builder()
                .status("ERROR")
                .message("Invalid password.")
                .build();
    }

    private CommandResponse logout(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length != CommandType.LOGOUT.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("logout expects 0 arguments.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
        }

        if (!loggedInUsers.contains(user)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("User is not logged in.")
                    .build();
        }

        loggedInUsers.remove(user);
        return CommandResponse.builder()
                .status("OK")
                .message("User logged out successfully.")
                .build();
    }

    private CommandResponse search(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length < CommandType.SEARCH.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("search expects at least 1 argument.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
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
        return CommandResponse.builder()
                .status("OK")
                .message(String.format("Search has returned %s songs.", results.size()))
                .data(results)
                .build();
    }

    private CommandResponse createPlaylist(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length != CommandType.CREATE_PLAYLIST.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("create-playlist expects 1 argument.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
        }

        if (user.getPlaylists().stream().anyMatch(playlist -> playlist.name().equals(args[0]))) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Playlist with this name already exists.")
                    .build();
        }

        user.getPlaylists().add(Playlist.of(args[0]));
        return CommandResponse.builder()
                .status("OK")
                .message("Playlist created successfully.")
                .build();
    }

    private CommandResponse addSongTo(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length != CommandType.ADD_SONG_TO.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("add-song-to expects 2 arguments.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
        }

        Playlist playlist = user.getPlaylists().stream()
                .filter(p -> p.name().equals(args[0]))
                .findFirst()
                .orElse(null);
        if (playlist == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Playlist with this name doesn't exists.")
                    .build();
        }

        int songIndex;
        try {
            songIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid song index.")
                    .build();
        }

        Song toAdd = songs.stream()
                .filter(s -> s.index() == songIndex)
                .findFirst()
                .orElse(null);
        if (toAdd == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Song with this index doesn't exists.")
                    .build();
        }

        if (playlist.songs().contains(toAdd)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Song is already in the playlist.")
                    .build();
        }

        playlist.songs().add(toAdd);
        return CommandResponse.builder()
                .status("OK")
                .message("Song added successfully.")
                .build();
    }

    private CommandResponse showPlaylist(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length != CommandType.SHOW_PLAYLIST.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("show-playlist expects 1 argument.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
        }

        Playlist playlist = user.getPlaylists().stream()
                .filter(p -> p.name().equals(args[0]))
                .findFirst()
                .orElse(null);
        if (playlist == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Playlist with this name doesn't exists.")
                    .build();
        }

        return CommandResponse.builder()
                .status("OK")
                .message(String.format("Playlist %s contains %s song(s).", playlist.name(), playlist.songs().size()))
                .data(playlist.songs())
                .build();
    }

    public CommandResponse play(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length != CommandType.PLAY.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("play expects 1 argument.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
        }

        int songIndex;
        try {
            songIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid song index.")
                    .build();
        }

        Song song = songs.stream()
                .filter(s -> s.index() == songIndex)
                .findFirst()
                .orElse(null);
        if (song == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Song with this index doesn't exists.")
                    .build();
        }

        // TODO: Implement playing a song

        return CommandResponse.builder()
                .status("OK")
                .message(String.format("Playing %s by %s", song.name(), song.artist()))
                .build();
    }

    public CommandResponse top(String[] args, AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("You are not logged in.")
                    .build();
        }
        if (args.length != CommandType.TOP.getArgumentsCount()) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("top expects 1 argument.")
                    .build();
        }

        User user = users.getUser(accessKey.username());
        if (user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid access key.")
                    .build();
        }

        int topCount;
        try {
            topCount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.builder()
                    .status("ERROR")
                    .message("Invalid top count.")
                    .build();
        }

        // TODO: Implement top mechanic

        return CommandResponse.builder()
                .status("OK")
                .message("Top songs.")
                .data(List.of())
                .build();
    }

}