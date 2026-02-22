package tests;

import managers.UserManager;
import models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
    }

    @Test
    void addUser_success() {
        User user = User.validate("levon666", "Levon Simonian", "lovonsada@mail.com");

        userManager.add(user);

        assertEquals(1, userManager.count());
        assertTrue(userManager.exists("levon666"));
    }

    @Test
    void addDuplicateUser_shouldThrow() {
        User user = User.validate("levon666", "Levon Simonian", "lovonsada@mail.com");

        userManager.add(user);

        assertThrows(IllegalArgumentException.class,
                () -> userManager.add(user));
    }

    @Test
    void findByUsername_success() {
        User user = User.validate("levon666", "Levon Simonian", "lovonsada@mail.com");
        userManager.add(user);

        Optional<User> found = userManager.findByUsername("levon666");

        assertTrue(found.isPresent());
        assertEquals("levon666", found.get().username());
    }

    @Test
    void updateUser_success() {
        User user = User.validate("levon666", "Levon Simonian", "lovonsada@mail.com");
        userManager.add(user);

        userManager.update("levon666", "Liivon Simonian", "livinsada@mail.com");

        User updated = userManager.findByUsername("levon666").get();

        assertEquals("Liivon Simonian", updated.fullName());
        assertEquals("livinsada@mail.com", updated.email());
    }

    @Test
    void removeUser_success() {
        User user = User.validate("levon666", "Levon Simonian", "lovonsada@mail.com");
        userManager.add(user);

        userManager.remove(user);

        assertEquals(0, userManager.count());
    }
}