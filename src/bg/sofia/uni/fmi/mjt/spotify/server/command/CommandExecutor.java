package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.NetworkTools;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandType;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.StreamTransport;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.NoFreePortAvailableException;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;
import bg.sofia.uni.fmi.mjt.spotify.server.TCPStreamServer;
import bg.sofia.uni.fmi.mjt.spotify.server.UDPStreamServer;
import bg.sofia.uni.fmi.mjt.spotify.server.users.User;
import bg.sofia.uni.fmi.mjt.spotify.server.users.UserStorage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class CommandExecutor {
    private final UserStorage users;
    private final List<Song> songs;
    private final ExecutorService threadExecutor;
    private final Map<SocketAddress, Future<?>> socketThreads;

    public CommandExecutor(UserStorage users, List<Song> songs, ExecutorService threadExecutor) {
        if (users == null || songs == null) {
            throw new IllegalArgumentException("Users and songs cannot be null.");
        }
        this.users = users;
        this.songs = songs;
        this.threadExecutor = threadExecutor;
        this.socketThreads = new HashMap<>();
    }

    public CommandResponse execute(Command cmd) {
        SpotifyLogger.logger().fine(String.format("User %s executed command %s",
                                        cmd.accessKey() != null ? cmd.accessKey().username() : "ANON",
                                        cmd.command()));
        if (cmd.command().requiresAuthentication()) {
            CommandResponse errAuthResponse = validateAuthentication(cmd.accessKey());
            if (errAuthResponse != null) {
                return errAuthResponse;
            }
        }

        return switch (cmd.command()) {
            case REGISTER -> register(cmd.arguments());
            case LOGIN -> login(cmd.arguments());
            case SEARCH -> search(cmd.arguments());
            case TOP -> top(cmd.arguments());
            case CREATE_PLAYLIST -> createPlaylist(cmd.arguments(), cmd.accessKey());
            case ADD_SONG_TO -> addSongTo(cmd.arguments(), cmd.accessKey());
            case SHOW_PLAYLIST -> showPlaylist(cmd.arguments(), cmd.accessKey());
            case PLAY -> play(cmd.arguments(), cmd.originAddress(), cmd.originSocket());
            case STOP -> stop(cmd.originSocket());
        };
    }

    private CommandResponse validateAuthentication(AccessKey accessKey) {
        if (accessKey == null) {
            return CommandResponse.builder().buildError("You are not logged in.");
        }

        User user = users.getUser(accessKey.username());
        if (!user.isAccessKeyValid(accessKey)) {
            return CommandResponse.builder().buildError("Invalid access key.");
        }
        return null;
    }

    private CommandResponse register(String[] args) {
        int argCount = CommandType.REGISTER.getArgumentsCount();
        if (args.length != argCount) {
            return CommandResponse.builder().buildError(String.format("register expects %s arguments.", argCount));
        }

        String email = args[0];
        String password = args[1];

        users.addUser(User.of(email, password));

        return CommandResponse.builder().buildOK("User registered successfully.");
    }

    private CommandResponse login(String[] args) {
        int argCount = CommandType.LOGIN.getArgumentsCount();
        if (args.length != argCount) {
            return CommandResponse.builder().buildError(String.format("login expects %s arguments.", argCount));
        }

        String email = args[0];
        String password = args[1];
        User user = users.getUser(email);
        if (user == null) {
            return CommandResponse.builder().buildError("User with this email doesn't exists.");
        }

        if (user.validatePassword(password)) {
            return CommandResponse.builder()
                    .status("OK")
                    .message("User logged in successfully.")
                    .accessKey(user.getAccessKey())
                    .build();
        }

        return CommandResponse.builder().buildError("Invalid password.");
    }

    private CommandResponse search(String[] args) {
        if (args.length < CommandType.SEARCH.getArgumentsCount()) {
            return CommandResponse.builder().buildError("search expects at least 1 argument.");
        }

        Map<Integer, Song> results = songs.stream()
                .filter(song -> {
                    for (String keyword : args) {
                        if (song.name().toLowerCase().contains(keyword.toLowerCase()) ||
                            song.artist().toLowerCase().contains(keyword.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toMap(songs::indexOf, song -> song));


        return CommandResponse.builder()
                .status("OK")
                .message(String.format("Search has returned %s songs.", results.size()))
                .data(results)
                .build();
    }

    private CommandResponse createPlaylist(String[] args, AccessKey accessKey) {
        if (args.length != CommandType.CREATE_PLAYLIST.getArgumentsCount()) {
            return CommandResponse.builder().buildError("create-playlist expects 1 argument.");
        }

        User user = users.getUser(accessKey.username());

        if (user.getPlaylists().stream().anyMatch(playlist -> playlist.name().equals(args[0]))) {
            return CommandResponse.builder().buildError("Playlist with this name already exists.");
        }

        user.getPlaylists().add(Playlist.of(args[0]));
        return CommandResponse.builder().buildOK("Playlist created successfully.");
    }

    private CommandResponse addSongTo(String[] args, AccessKey accessKey) {
        if (args.length != CommandType.ADD_SONG_TO.getArgumentsCount()) {
            return CommandResponse.builder().buildError("add-song-to expects 2 arguments.");
        }

        User user = users.getUser(accessKey.username());

        Playlist playlist = user.getPlaylists().stream()
                .filter(p -> p.name().equals(args[0]))
                .findFirst()
                .orElse(null);
        if (playlist == null) {
            return CommandResponse.builder().buildError("Playlist with this name doesn't exists.");
        }

        int songIndex;
        try {
            songIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return CommandResponse.builder().buildError("Invalid song index.");
        }

        Song toAdd = songs.stream()
                .filter(s -> songs.indexOf(s) == songIndex)
                .findFirst()
                .orElse(null);
        if (toAdd == null) {
            return CommandResponse.builder().buildError("Song with this index doesn't exists.");
        }

        if (playlist.songs().contains(toAdd)) {
            return CommandResponse.builder().buildError("Song is already in playlist.");
        }

        playlist.songs().add(toAdd);
        return CommandResponse.builder().buildOK("Song added to playlist.");
    }

    private CommandResponse showPlaylist(String[] args, AccessKey accessKey) {
        if (args.length != CommandType.SHOW_PLAYLIST.getArgumentsCount()) {
            return CommandResponse.builder().buildError("show-playlist expects 1 argument.");
        }

        User user = users.getUser(accessKey.username());

        Playlist playlist = user.getPlaylists().stream()
                .filter(p -> p.name().equals(args[0]))
                .findFirst()
                .orElse(null);
        if (playlist == null) {
            return CommandResponse.builder().buildError("Playlist with this name doesn't exists.");
        }

        Map<Integer, Song> result = playlist.songs().stream()
                .collect(Collectors.toMap(songs::indexOf, song -> song));

        return CommandResponse.builder()
                .status("OK")
                .message(String.format("Playlist %s contains %s song(s).", playlist.name(), playlist.songs().size()))
                .data(result)
                .build();
    }

    private CommandResponse play(String[] args, InetAddress clientAddress, SocketAddress clientSocketAddress) {
        if (args.length < CommandType.PLAY.getArgumentsCount()) {
            return CommandResponse.builder().buildError("play expects 1 argument.");
        }

        int songIndex;
        try {
            songIndex = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.builder().buildError("Invalid song index.");
        }

        Song song = songs.stream()
                .filter(s -> songs.indexOf(s) == songIndex)
                .findFirst()
                .orElse(null);
        if (song == null) {
            return CommandResponse.builder().buildError("Song with this index doesn't exists.");
        }

        if (socketThreads.containsKey(clientSocketAddress)) {
            if (socketThreads.get(clientSocketAddress).isDone()) {
                socketThreads.remove(clientSocketAddress);
            } else {
                return CommandResponse.builder().buildError("You are already playing a song.");
            }
        }

        AudioFormat format = null;
        try {
            format = AudioSystem.getAudioFileFormat(song.path().toFile()).getFormat();
        } catch (IOException e) {
            SpotifyLogger.logger().log(Level.WARNING, "Failed to get audio format for song: " + song.name(), e);
        } catch (UnsupportedAudioFileException e) {
            SpotifyLogger.logger().warning("Unsupported audio format for song: " + song.path());
        }
        if (format == null) {
            return CommandResponse.builder().buildError("Failed to play song.");
        }

        StreamTransport streamTransport = StreamTransport.of(args[1]);
        int port;
        try{
            port = switch (streamTransport) {
                case TCP -> NetworkTools.findFreePort();
                case UDP -> Integer.parseInt(args[2]);
            };
        } catch (NumberFormatException e) {
            return CommandResponse.builder().buildError("Server: Malformed request from client.");
        } catch (NoFreePortAvailableException e) {
            return CommandResponse.builder().buildError("Couldn't open channel for streaming.");
        }
        Runnable task = switch (streamTransport) {
            case TCP -> new TCPStreamServer(port, song.path());
            case UDP -> new UDPStreamServer(port, clientAddress, song.path(), format);
        };
        socketThreads.put(clientSocketAddress, threadExecutor.submit(task));
        song.incrementStreams();

        return CommandResponse.builder()
                .status(String.format("OK-PORT-%s", port))
                .message(String.format("Playing %s by %s", song.name(), song.artist()))
                .audioFormat(format)
                .transport(streamTransport)
                .build();
    }

    private CommandResponse stop(SocketAddress clientSocketAddress) {
        if (!socketThreads.containsKey(clientSocketAddress)) {
            return CommandResponse.builder().buildError("You are not playing a song.");
        }

        socketThreads.get(clientSocketAddress).cancel(true);
        socketThreads.remove(clientSocketAddress);

        return CommandResponse.builder().buildOK("Streaming stopped.");
    }

    private CommandResponse top(String[] args) {
        if (args.length != CommandType.TOP.getArgumentsCount()) {
            return CommandResponse.builder().buildError("top expects 1 argument.");
        }

        int topCount;
        try {
            topCount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return CommandResponse.builder().buildError("Invalid top count.");
        }

        List<Song> topSongs = songs.stream()
                .sorted((s1, s2) -> s2.streams() - s1.streams())
                .limit(topCount)
                .toList();

        Map<Integer, Song> result = new LinkedHashMap<>();
        for (Song song : topSongs) {
            result.put(songs.indexOf(song), song);
        }

        return CommandResponse.builder()
                .status("OK")
                .message("Top songs.")
                .data(result)
                .build();
    }
}