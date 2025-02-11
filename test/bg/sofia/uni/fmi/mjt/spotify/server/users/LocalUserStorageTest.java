package bg.sofia.uni.fmi.mjt.spotify.server.users;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalUserStorageTest {
    private static final Path TEST_FILE = Path.of("test.json");
    private List<Song> songs;

    @BeforeEach
    public void setUp() throws IOException {
        songs = new ArrayList<>();
        try {
            Files.delete(TEST_FILE);
        } catch (NoSuchFileException _) {}
    }

    @AfterAll
    public static void tearDown() throws IOException {
        try {
            Files.delete(TEST_FILE);
        } catch (NoSuchFileException _) {}
    }

    @Test
    public void testConstructorNoFile() {
        LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs);
        storage.close();
        assertTrue(Files.exists(TEST_FILE));
    }

    @Test
    public void testConstructorLoadUsers() {
        String fileData = "[{\"email\":\"username\",\"passwordHash\":\"qAtWiiN/UDkdLx+Xvq+ZVk4z0uHIouXKwhztpwFXAxI\\u003d\",\"playlists\":[]}]";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_FILE.toFile()))) {
            writer.write(fileData);
        } catch (IOException e) {
            fail("Failed to write to file");
        }

        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            User user = storage.getUser("username");
            assertTrue(Files.exists(TEST_FILE));
            assertNotNull(user);
        }
    }

    @Test
    void testAddUser() {
        User newUser = new User("username", "password");
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            storage.addUser(newUser);
        }
        assertTrue(Files.exists(TEST_FILE));

        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_FILE.toFile()))) {
            String line = reader.readLine();
            assertTrue(line.contains(newUser.getEmail()));
        } catch (IOException e) {
            fail("Failed to read from file");
        }
    }

    @Test
    void testAddUserNull() {
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            assertThrows(IllegalArgumentException.class, () -> storage.addUser(null));
        }
    }

    @Test
    void testAddUserExists() {
        User user = new User("username", "password");
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            storage.addUser(user);
            assertTrue(storage.containsUser(user));
            assertThrows(IllegalArgumentException.class, () -> storage.addUser(user));
        }
    }

    @Test
    void testRemoveUser() {
        User user = new User("username", "password");
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            storage.addUser(user);
            assertTrue(storage.containsUser(user));
            storage.removeUser(user);
            assertFalse(storage.containsUser(user));
        }
    }

    @Test
    void testRemoveUserNull() {
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            assertThrows(IllegalArgumentException.class, () -> storage.removeUser(null));
        }
    }

    @Test
    void testRemoveUserNonexistent() {
        User user = new User("username", "password");
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            storage.addUser(user);
            assertTrue(storage.containsUser(user));
            storage.removeUser(user);
            assertFalse(storage.containsUser(user));
            assertThrows(IllegalArgumentException.class, () -> storage.removeUser(user));
        }
    }

    @Test
    void testContainsUserNull() {
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            assertThrows(IllegalArgumentException.class, () -> storage.containsUser(null));
        }
    }

    @Test
    void testGetUser() {
        User user = new User("username", "password");
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            storage.addUser(user);
            User retrieved = storage.getUser("username");
            assertEquals(user, retrieved);
        }
    }

    @Test
    public void testGetUserNull() {
        try (LocalUserStorage storage = new LocalUserStorage(TEST_FILE, songs)) {
            assertThrows(IllegalArgumentException.class, () -> storage.getUser(null));
        }
    }
}