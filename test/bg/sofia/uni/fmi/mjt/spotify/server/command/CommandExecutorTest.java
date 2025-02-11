package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.*;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.spotify.server.users.User;
import bg.sofia.uni.fmi.mjt.spotify.server.users.UserStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

public class CommandExecutorTest {
    private CommandExecutor commandExecutor;
    private UserStorage userStorage;
    private List<Song> songs;
    private ExecutorService threadExecutor;
    private SocketAddress clientSocketAddress;

    @BeforeEach
    void setUp() {
        userStorage = new MemoryUserStorageStub();
        userStorage.addUser(User.of("username", "password"));
        songs = new ArrayList<>();
        threadExecutor = Mockito.mock(ExecutorService.class);
        commandExecutor = new CommandExecutor(userStorage, songs, threadExecutor);
        clientSocketAddress = Mockito.mock(SocketAddress.class);
    }

    @Test
    void testConstructorUsersNull() {
        assertThrows(IllegalArgumentException.class, () -> new CommandExecutor(null, songs, threadExecutor),
                "User storage cannot be null.");
    }

    @Test
    void testConstructorSongsNull() {
        assertThrows(IllegalArgumentException.class, () -> new CommandExecutor(userStorage, null, threadExecutor),
                "Songs cannot be null.");
    }

    @Test
    void testAuthentication() throws InvalidCommandException {
        ClientRequest request = ClientRequest.of("login username password", null);

        Command command = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(command);
        assertEquals("OK", response.status(), "Status should be \"OK\".");
        assertEquals("User logged in successfully.", response.message(), "Message doesn't match");
        assertNotNull(response.accessKey(), "Access key should not be null.");
    }

    @Test
    void testAuthenticationInvalidCredential() throws InvalidCommandException {
        ClientRequest request = ClientRequest.of("login username2 password", null);

        Command command = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(command);
        assertEquals("ERROR", response.status(), "Status should be \"ERROR\".");
        assertEquals("User with this email doesn't exists.", response.message(), "Message doesn't match");
        assertNull(response.accessKey(), "Access key should be null.");
    }

    @Test
    void testAuthenticationInvalidPassword() throws InvalidCommandException {
        ClientRequest request = ClientRequest.of("login username wrong_password", null);

        Command command = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(command);
        assertEquals("ERROR", response.status(), "Status should be \"ERROR\".");
        assertEquals("Invalid password.", response.message(), "Message doesn't match");
        assertNull(response.accessKey(), "Access key should be null.");
    }

    @Test
    public void testAuthenticatedCheckNoAccessKey() throws InvalidCommandException {
        // Executing a protected command, any one works
        ClientRequest request = ClientRequest.of("create-playlist something", null);

        Command command = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(command);
        assertEquals("ERROR", response.status(), "Status should be \"ERROR\".");
        assertEquals("You are not logged in.", response.message(), "Message doesn't match");
        assertNull(response.accessKey(), "Access key should be null.");
    }

    @Test
    public void testAuthenticatedCheckInvalidAccessKey() throws InvalidCommandException {
        // Executing a protected command, any one works
        ClientRequest request = ClientRequest.of(
                "create-playlist something",
                AccessKey.of("username", "fake"));

        Command command = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(command);
        assertEquals("ERROR", response.status(), "Status should be \"ERROR\".");
        assertEquals("Invalid access key.", response.message(), "Message doesn't match");
        assertNull(response.accessKey(), "Access key should be null.");
    }

    @Test
    public void testRegister() throws InvalidCommandException {
        String newUserEmail = "newUser@mail.bg";
        String newUserPassword = "newPassword";
        User newUser = User.of(newUserEmail, newUserPassword);
        String command = String.format("register %s %s", newUserEmail, newUserPassword);
        ClientRequest request = ClientRequest.of(command, null);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("OK", response.status(), "Status should be \"OK\".");
        assertEquals("User registered successfully.", response.message(), "Message doesn't match");

        assertTrue(userStorage.containsUser(newUser));
        assertEquals(newUserEmail, userStorage.getUser(newUserEmail).getEmail());
    }

    @Test
    public void testRegisterWrongArgsCount() throws InvalidCommandException {
        String command = "register";
        ClientRequest request = ClientRequest.of(command, null);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status(), "Status should be \"ERROR\".");
        assertTrue(response.message().startsWith("register expects "), "Message doesn't match");
    }

    public AccessKey login() throws InvalidCommandException {
        ClientRequest request = ClientRequest.of("login username password", null);

        Command command = Command.builder()
                .command(request.command())
                .accessKey(request.accessKey())
                .originAddress(InetAddress.getLoopbackAddress())
                .originSocket(clientSocketAddress)
                .build();

        CommandResponse response = commandExecutor.execute(command);
        return response.accessKey();
    }

    @Test
    public void testLogin() throws InvalidCommandException {
        ClientRequest request = ClientRequest.of("login username password", null);

        Command command = Command.builder()
                .command(request.command())
                .accessKey(request.accessKey())
                .originAddress(InetAddress.getLoopbackAddress())
                .originSocket(clientSocketAddress)
                .build();

        CommandResponse response = commandExecutor.execute(command);
        assertEquals("OK", response.status(), "Status should be \"OK\".");
        assertEquals("User logged in successfully.", response.message(), "Message doesn't match");
        assertNotNull(response.accessKey(), "Access key should not be null.");
    }

    @Test
    public void testLoginWrongArgsCount() throws InvalidCommandException {
        String command = "login";
        ClientRequest request = ClientRequest.of(command, null);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status(), "Status should be \"ERROR\".");
        assertTrue(response.message().startsWith("login expects "), "Message doesn't match");
    }

    @Test
    public void testSearch() throws InvalidCommandException {
        Song song1 = Song.of("1", "artist1", "lorem ipsum", Path.of("song1.wav"));
        Song song2 = Song.of("2", "artist2", "dolor lorem", Path.of("song2.wav"));
        Song song3 = Song.of("3", "artist2", "amet", Path.of("song3.wav"));
        songs.addAll(List.of(song1, song2, song3));

        String command = "search lorem";
        ClientRequest request = ClientRequest.of(command, null);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        List<Song> expected = List.of(song1, song2);
        List<Song> found = response.data().values().stream().toList();
        assertEquals("OK", response.status(), "Status should be \"OK\".");
        assertEquals(expected.size(), response.data().size(), "Data size doesn't match");
        assertEquals(expected, found, "Data doesn't match");
    }

    @Test
    public void testSearchArgsCount() throws InvalidCommandException {
        String command = "search";
        ClientRequest request = ClientRequest.of(command, null);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertTrue(response.message().startsWith("search expects "));
    }

    @Test
    public void testCreatePlaylist() throws InvalidCommandException {
        String playlistName = "NewPlaylist";
        String command = String.format("create-playlist %s", playlistName);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("OK", response.status());
        assertEquals("Playlist created successfully.", response.message());

        User user = userStorage.getUser(accessKey.username());
        assertTrue(user.getPlaylists().stream().anyMatch(playlist -> playlist.name().equals(playlistName)));
    }

    @Test
    public void testCreatePlaylistArgsCount() throws InvalidCommandException {
        String command = "create-playlist";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertTrue(response.message().startsWith("create-playlist expects "));
    }

    @Test
    public void testCreatePlaylistAlreadyExists() throws InvalidCommandException {
        String playlistName = "NewPlaylist";
        String command = String.format("create-playlist %s", playlistName);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        User user = userStorage.getUser(accessKey.username());
        user.getPlaylists().add(Playlist.of(playlistName));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Playlist with this name already exists.", response.message());
    }

    @Test
    public void testAddSongTo() throws InvalidCommandException {
        String playlistName = "MyPlaylist";
        Song song = Song.of("1", "artist", "pretty song", Path.of("song.wav"));
        songs.add(song);
        String command = String.format("add-song-to %s %s", playlistName, "0");

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        User user = userStorage.getUser(accessKey.username());
        user.getPlaylists().add(Playlist.of(playlistName));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("OK", response.status());
        assertEquals("Song added to playlist.", response.message());

        Playlist playlist = user.getPlaylists().stream().filter(p -> p.name().equals(playlistName)).findFirst().orElse(null);
        assertNotNull(playlist);
        assertTrue(playlist.songs().contains(song));
    }

    @Test
    public void testAddSongToArgsCount() throws InvalidCommandException {
        String playlistName = "MyPlaylist";
        String command = String.format("add-song-to %s", playlistName);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertTrue(response.message().startsWith("add-song-to expects "), "");
    }

    @Test
    public void testAddSongToPlaylistNonexistent() throws InvalidCommandException {
        String playlistName = "MyPlaylist2";
        String command = String.format("add-song-to %s %s", playlistName, "0");

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Playlist with this name doesn't exists.", response.message());
    }

    @Test
    public void testAddSongToSongIndexInvalid() throws InvalidCommandException {
        String playlistName = "MyPlaylist";
        String command = String.format("add-song-to %s %s", playlistName, "notanumber");

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        User user = userStorage.getUser(accessKey.username());
        user.getPlaylists().add(Playlist.of(playlistName));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Invalid song index.", response.message());
    }

    @Test
    public void testAddSongToSongNotFound() throws InvalidCommandException {
        String playlistName = "MyPlaylist";
        Song song = Song.of("1", "artist", "pretty song", Path.of("song.wav"));
        songs.add(song);
        String command = String.format("add-song-to %s %s", playlistName, "10");

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        User user = userStorage.getUser(accessKey.username());
        user.getPlaylists().add(Playlist.of(playlistName));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Song with this index doesn't exists.", response.message());
    }

    @Test
    public void testAddSongToAlreadyExists() throws InvalidCommandException {
        String playlistName = "MyPlaylist";
        Song song = Song.of("1", "artist", "pretty song", Path.of("song.wav"));
        songs.add(song);
        String command = String.format("add-song-to %s %s", playlistName, "0");

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        User user = userStorage.getUser(accessKey.username());
        user.getPlaylists().add(Playlist.of(playlistName));
        Playlist playlist = user.getPlaylists().stream().filter(p -> p.name().equals(playlistName)).findFirst().orElse(null);
        assertNotNull(playlist);
        playlist.songs().add(song);
        assertTrue(playlist.songs().contains(song));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Song is already in playlist.", response.message());
    }

    @Test
    public void testShowPlaylist() throws InvalidCommandException {
        String playlistName = "MyPlaylist";
        String command = String.format("show-playlist %s", playlistName);
        Song song = Song.of("1", "artist", "pretty song", Path.of("song.wav"));
        songs.add(song);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        User user = userStorage.getUser(accessKey.username());
        user.getPlaylists().add(Playlist.of(playlistName));
        Playlist playlist = user.getPlaylists().stream().filter(p -> p.name().equals(playlistName)).findFirst().orElse(null);
        assertNotNull(playlist);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        String expectedMessage = String.format("Playlist %s contains %s song(s).", playlist.name(), playlist.songs().size());

        assertEquals("OK", response.status());
        assertEquals(expectedMessage, response.message());
        assertEquals(playlist.songs(), response.data().values().stream().toList());
    }

    @Test
    public void testShowPlaylistArgsCount() throws InvalidCommandException {
        String command = "show-playlist";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertTrue(response.message().startsWith("show-playlist expects "));
    }

    @Test
    public void testShowPlaylistNonExistent() throws InvalidCommandException {
        String command = "show-playlist nonexistent";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Playlist with this name doesn't exists.", response.message());
    }

    @Test
    public void testTop() throws InvalidCommandException {
        String command = String.format("top %s", 1);
        Song song1 = new Song("1", "artist", "pretty song", Path.of("song1.wav"), 2);
        Song song2 = new Song("2", "artist", "popular song", Path.of("song2.wav"), 100);
        songs.addAll(List.of(song1, song2));

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        String expectedMessage = String.format("Top %s song(s).", 1);

        assertEquals("OK", response.status());
        assertEquals(expectedMessage, response.message());
        assertEquals(List.of(song2), response.data().values().stream().toList());
    }

    @Test
    public void testTopArgsCount() throws InvalidCommandException {
        String command = "top";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertTrue(response.message().startsWith("top expects "));
    }

    @Test
    public void testTopInvalidCount() throws InvalidCommandException {
        String command = "top notanumeber";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Invalid top count.", response.message());
    }

    @Test
    public void testPlayTCP() throws InvalidCommandException {
        Path songPath = Path.of("test/bg/sofia/uni/fmi/mjt/spotify/server/command/test.wav");
        Song song = Song.of("1", "artist", "pretty song", songPath);
        songs.add(song);
        String command = String.format("play %s tcp", 0);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        Mockito.when(threadExecutor.submit(Mockito.any(Runnable.class)))
                .thenReturn(Mockito.mock(Future.class));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertTrue(response.status().startsWith("OK-PORT-"));
        assertEquals(String.format("Playing %s by %s", song.name(), song.artist()), response.message());
        Mockito.verify(threadExecutor).submit(Mockito.any(Runnable.class));
    }

    @Test
    public void testPlayUDP() throws InvalidCommandException {
        Path songPath = Path.of("test/bg/sofia/uni/fmi/mjt/spotify/server/command/test.wav");
        Song song = Song.of("1", "artist", "pretty song", songPath);
        songs.add(song);
        String command = String.format("play %s udp 78962", 0);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        Mockito.when(threadExecutor.submit(Mockito.any(Runnable.class)))
                .thenReturn(Mockito.mock(Future.class));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertTrue(response.status().startsWith("OK-PORT-"));
        assertEquals(String.format("Playing %s by %s", song.name(), song.artist()), response.message());
        Mockito.verify(threadExecutor).submit(Mockito.any(Runnable.class));
    }

    @Test
    public void testPlayArgsCount() throws InvalidCommandException {
        String command = "play";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertTrue(response.message().startsWith("play expects "));
    }

    @Test
    public void testPlayInvalidSongIndex() throws InvalidCommandException {
        String command = "play notanumber tcp";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Invalid song index.", response.message());
    }

    @Test
    public void testPlaySongNotFound() throws InvalidCommandException {
        String command = "play 101 tcp";
        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Song with this index doesn't exists.", response.message());
    }

    @Test
    public void testPlayAlreadyPlaying() throws InvalidCommandException {
        Path songPath = Path.of("test/bg/sofia/uni/fmi/mjt/spotify/server/command/test.wav");
        Song song = Song.of("1", "artist", "pretty song", songPath);
        songs.add(song);
        String command = String.format("play %s tcp", 0);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);
        Mockito.when(threadExecutor.submit(Mockito.any(Runnable.class)))
                .thenReturn(Mockito.mock(Future.class));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertTrue(response.status().startsWith("OK-PORT-"));
        assertEquals(String.format("Playing %s by %s", song.name(), song.artist()), response.message());

        CommandResponse response2 = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response2.status());
        assertEquals("You are already playing a song.", response2.message());
    }

    @Test
    public void testPlayMalformedRequest() throws InvalidCommandException {
        Path songPath = Path.of("test/bg/sofia/uni/fmi/mjt/spotify/server/command/test.wav");
        songs.add(Song.of("1", "artist", "pretty song", songPath));
        String command = "play 0 udp notanumber";

        ClientRequest request = ClientRequest.of(command, login());

        Command commandObj = Command.builder()
                .command(request.command())
                .accessKey(request.accessKey())
                .originAddress(InetAddress.getLoopbackAddress())
                .originSocket(clientSocketAddress)
                .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("Server: Malformed request from client.", response.message());
    }

    @Test
    public void testStop() throws InvalidCommandException {
        Path songPath = Path.of("test/bg/sofia/uni/fmi/mjt/spotify/server/command/test.wav");
        Song song = Song.of("1", "artist", "pretty song", songPath);
        songs.add(song);
        String playCommand = String.format("play %s tcp", 0);

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(playCommand, accessKey);
        Mockito.when(threadExecutor.submit(Mockito.any(Runnable.class)))
                .thenReturn(Mockito.mock(Future.class));

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertTrue(response.status().startsWith("OK-PORT-"));
        assertEquals(String.format("Playing %s by %s", song.name(), song.artist()), response.message());

        String stopCommand = "stop";
        ClientRequest stopRequest = ClientRequest.of(stopCommand, accessKey);

        commandObj = Command.builder()
            .command(stopRequest.command())
            .accessKey(stopRequest.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        response = commandExecutor.execute(commandObj);
        assertEquals("OK", response.status());
        assertEquals("Streaming stopped.", response.message());
    }

    @Test
    public void testStopNotPlaying() throws InvalidCommandException {
        String command = "stop";

        AccessKey accessKey = login();
        ClientRequest request = ClientRequest.of(command, accessKey);

        Command commandObj = Command.builder()
            .command(request.command())
            .accessKey(request.accessKey())
            .originAddress(InetAddress.getLoopbackAddress())
            .originSocket(clientSocketAddress)
            .build();

        CommandResponse response = commandExecutor.execute(commandObj);
        assertEquals("ERROR", response.status());
        assertEquals("You are not playing a song.", response.message());
    }

}
