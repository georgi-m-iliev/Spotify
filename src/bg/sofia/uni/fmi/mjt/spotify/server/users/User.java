package bg.sofia.uni.fmi.mjt.spotify.server.users;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Playlist;

import java.util.ArrayList;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class User {
    private final String email;
    private String passwordHash;
    private final List<Playlist> playlists;
    private transient String accessKey;

    public User(String email, String password) {
        this.email = email;
        this.passwordHash = hash(password);
        this.playlists = new ArrayList<>();
    }

    public String getEmail() {
        return email;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void editPassword(String newPassword) {
        this.passwordHash = hash(newPassword);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash value", e);
        }
    }

    /**
     * Validates the supplied password by hashing it and comparing it to the expected hash.
     * @param password the password to validate
     * @param expectedHash the expected hash of the password
     * @return true if the hashes match, false otherwise
     */
    private static boolean validatePassword(String password, String expectedHash) {
        String hash = hash(password);
        return hash.equals(expectedHash);
    }

    /**
     * Validates the supplied password against the user's stored password hash.
     * @param password the password to validate
     * @return true if the password is correct, false otherwise
     */
    public boolean validatePassword(String password) {
        boolean result = validatePassword(password, passwordHash);
        if (result) {
            accessKey = hash(email + password + System.currentTimeMillis());
        }
        return validatePassword(password, passwordHash);
    }

    /**
     * Returns the access key for the user.
     * @return AccessKey object
     */
    public AccessKey getAccessKey() {
        return AccessKey.of(email, accessKey);
    }

    /**
     * Checks if the access key is valid for the user.
     * @param key the access key to check
     * @return true if the access key is valid, false otherwise
     */
    public boolean isAccessKeyValid(AccessKey key) {
        return key.token().equals(accessKey);
    }

    public static User of(String email, String password) {
        return new User(email, password);
    }
}
