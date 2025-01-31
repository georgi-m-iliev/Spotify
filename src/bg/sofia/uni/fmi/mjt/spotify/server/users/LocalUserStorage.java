package bg.sofia.uni.fmi.mjt.spotify.server.users;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalUserStorage implements UserStorage, AutoCloseable {
    private static final Gson gson = new Gson();
    private final Path storagePath;
    private final List<User> users;

    public LocalUserStorage(Path storagePath) {
        this.storagePath = storagePath;
        this.users = new ArrayList<>();
        loadUsers();
    }

    private void loadUsers() {
        if (!storagePath.toFile().exists()) {
            return;
        }
        try (FileReader reader = new FileReader(storagePath.toFile())) {
            User[] users = gson.fromJson(reader, User[].class);
            if (users != null) {
                this.users.addAll(List.of(users));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users", e);
        }
    }

    private void saveUsers() {
        if (!storagePath.toFile().exists()) {
            try {
                storagePath.toFile().createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create storage file", e);
            }
        }
        try (FileWriter writer = new FileWriter(storagePath.toFile())) {
            gson.toJson(users, writer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save users", e);
        }
    }

    /**
     * Adds a user to the storage.
     * @param user the user to be added
     * @throws IllegalArgumentException if the user is null or already exists
     */
    @Override
    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (containsUser(user)) {
            throw new IllegalArgumentException("User already exists");
        }

        users.add(user);
    }

    /**
     * Removes a user from the storage.
     * @param user the user to be removed
     * @throws IllegalArgumentException if the user is null or does not exist
     */
    @Override
    public void removeUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!containsUser(user)) {
            throw new IllegalArgumentException("User does not exist");
        }

        users.remove(user);
    }

    /**
     * Checks if a user is present in the storage.
     * @param user the user to be checked
     * @return true if the user is present, false otherwise
     * @throws IllegalArgumentException if the user is null
     */
    @Override
    public boolean containsUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return users.contains(user);
    }

    /**
     * Gets a user by email.
     * @param email the email of the user
     * @return the user with the given email or null if no such user exists
     * @throws IllegalArgumentException if the email is null
     */
    @Override
    public User getUser(String email) {
        if (email == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return users.stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void close() {
        saveUsers();
    }
}
