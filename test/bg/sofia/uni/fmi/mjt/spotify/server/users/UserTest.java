package bg.sofia.uni.fmi.mjt.spotify.server.users;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void testValidatePassword() {
        User user = User.of("test", "test");
        assertTrue(user.validatePassword("test"));
    }

    @Test
    void testEquals() {
        User user1 = User.of("test", "test");
        User user2 = User.of("test", "test");
        assertEquals(user1, user2);
    }

    @Test
    void testHashCode() {
        User user1 = User.of("test", "test");
        User user2 = User.of("test", "test");
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}