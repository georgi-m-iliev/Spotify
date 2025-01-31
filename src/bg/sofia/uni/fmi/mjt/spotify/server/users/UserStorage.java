package bg.sofia.uni.fmi.mjt.spotify.server.users;

public interface UserStorage {
    /**
     * Adds a user to the storage.
     * @param user the user to be added
     */
    void addUser(User user);

    /**
     * Removes a user from the storage.
     * @param user the user to be removed
     */
    void removeUser(User user);

    /**
     * Checks if a user is present in the storage.
     * @param user the user to be checked
     * @return true if the user is present, false otherwise
     */
    boolean containsUser(User user);

    /**
     * Gets a user by username.
     * @param username the username of the user
     * @return the user with the given username or null if no such user exists
     */
    User getUser(String username);
}
