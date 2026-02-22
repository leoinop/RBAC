package sorters;

import models.User;
import java.util.Comparator;

public final class UserSorters {

    private UserSorters() {}

    public static Comparator<User> byUsername() {
        return Comparator.comparing(User::getUsername);
    }

    public static Comparator<User> byFullName() {
        return Comparator.comparing(User::getFullName);
    }

    public static Comparator<User> byEmail() {
        return Comparator.comparing(User::getEmail);
    }
}