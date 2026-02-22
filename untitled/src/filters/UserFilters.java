package filters;

import models.User;

public final class UserFilters {

    private UserFilters() {}

    public static UserFilter byUsername(String username) {
        return user -> user.getUsername().equals(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        return user -> user.getUsername()
                .toLowerCase()
                .contains(substring.toLowerCase());
    }

    public static UserFilter byEmail(String email) {
        return user -> user.getEmail().equals(email);
    }

    public static UserFilter byEmailDomain(String domain) {
        return user -> user.getEmail().endsWith(domain);
    }

    public static UserFilter byFullNameContains(String substring) {
        return user -> user.getFullName()
                .toLowerCase()
                .contains(substring.toLowerCase());
    }
}