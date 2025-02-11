package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.server.users.User;
import bg.sofia.uni.fmi.mjt.spotify.server.users.UserStorage;

import java.util.ArrayList;
import java.util.List;

public class MemoryUserStorageStub implements UserStorage {
    private final List<User> users;

    public MemoryUserStorageStub() {
        this.users = new ArrayList<>();
    }

    /**
     * Adds a user to the storage.
     *
     * @param user the user to be added
     */
    @Override
    public void addUser(User user) {
        users.add(user);
    }

    /**
     * Removes a user from the storage.
     *
     * @param user the user to be removed
     */
    @Override
    public void removeUser(User user) {
        users.remove(user);
    }

    /**
     * Checks if a user is present in the storage.
     *
     * @param user the user to be checked
     * @return true if the user is present, false otherwise
     */
    @Override
    public boolean containsUser(User user) {
        return users.contains(user);
    }

    /**
     * Gets a user by username.
     *
     * @param username the username of the user
     * @return the user with the given username or null if no such user exists
     */
    @Override
    public User getUser(String username) {
        return users.stream().filter(user -> user.getEmail().equals(username)).findFirst().orElse(null);
    }
}
