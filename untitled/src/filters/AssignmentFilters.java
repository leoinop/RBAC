package filters;

import models.*;

public final class AssignmentFilters {

    private AssignmentFilters() {}

    public static AssignmentFilter byUser(User user) {
        return a -> a.getUser().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return a -> a.getUser().getUsername().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return a -> a.getRole().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return a -> a.getRole().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return a -> !a.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return a -> {
            if (type.equalsIgnoreCase("PERMANENT"))
                return a instanceof PermanentAssignment;
            if (type.equalsIgnoreCase("TEMPORARY"))
                return a instanceof TemporaryAssignment;
            return false;
        };
    }
}