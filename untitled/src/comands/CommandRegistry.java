package src.commands;

import src.system.RBACSystem;
import models.User;
import models.Role;
import models.Permission;
import models.RoleAssignment;
import models.PermanentAssignment;
import models.TemporaryAssignment;
import models.AssignmentMetadata;
import src.filters.UserFilters;
import src.filters.RoleFilters;
import src.filters.AssignmentFilters;
import src.sorters.UserSorters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    private final CommandParser parser;
    private final RBACSystem system;
    private final Scanner scanner;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CommandRegistry(RBACSystem system, Scanner scanner) {
        this.system = system;
        this.scanner = scanner;
        this.parser = new CommandParser();
        registerAllCommands();
    }

    private void registerAllCommands() {
        // Команды управления пользователями
        registerUserCommands();
        // Команды управления ролями
        registerRoleCommands();
        // Команды управления назначениями
        registerAssignmentCommands();
        // Команды просмотра прав
        registerPermissionCommands();
        // Служебные команды
        registerUtilityCommands();
    }

    private void registerUserCommands() {
        parser.registerCommand("user-list", "List all users", (scanner, system) -> {
            List<User> users = system.getUserManager().findAll();
            System.out.println("\n========== USERS ==========");
            System.out.printf("%-20s %-25s %-30s%n", "Username", "Full Name", "Email");
            System.out.println("-".repeat(75));
            for (User user : users) {
                System.out.printf("%-20s %-25s %-30s%n",
                        user.username(), user.fullName(), user.email());
            }
            System.out.printf("Total: %d users%n", users.size());
        });

        parser.registerCommand("user-create", "Create new user", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Full name: ");
            String fullName = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();

            try {
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                System.out.println("User created successfully!");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user details", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            User user = userOpt.get();
            System.out.println("\n========== USER DETAILS ==========");
            System.out.println("Username: " + user.username());
            System.out.println("Full name: " + user.fullName());
            System.out.println("Email: " + user.email());

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
            System.out.println("\nRoles:");
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %s [%s] - %s%n", ra.role().getName(), ra.assignmentType(), status);
            }

            System.out.println("\nPermissions:");
            Set<Permission> perms = system.getAssignmentManager().getUserPermissions(user);
            for (Permission p : perms) {
                System.out.printf("  - %s on %s%n", p.name(), p.resource());
            }
        });

        parser.registerCommand("user-update", "Update user data", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            if (!system.getUserManager().exists(username)) {
                System.out.println("User not found");
                return;
            }

            System.out.print("New full name (leave empty to keep): ");
            String fullName = scanner.nextLine();
            System.out.print("New email (leave empty to keep): ");
            String email = scanner.nextLine();

            try {
                system.getUserManager().update(username,
                        fullName.isEmpty() ? null : fullName,
                        email.isEmpty() ? null : email);
                System.out.println("User updated successfully!");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete user", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            System.out.print("Confirm delete (yes/no): ");
            String confirm = scanner.nextLine();

            if (!confirm.equalsIgnoreCase("yes")) {
                System.out.println("Delete cancelled");
                return;
            }

            User user = userOpt.get();
            // Удаляем все назначения пользователя
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
            for (RoleAssignment ra : assignments) {
                system.getAssignmentManager().remove(ra);
            }

            system.getUserManager().remove(user);
            System.out.println("User deleted successfully!");
        });

        parser.registerCommand("user-search", "Search users by filter", (scanner, system) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By username (contains)");
            System.out.println("2. By email (contains)");
            System.out.println("3. By email domain");
            System.out.println("4. By full name (contains)");
            System.out.print("Choose filter: ");

            int choice = Integer.parseInt(scanner.nextLine());
            String input;
            List<User> results = new ArrayList<>();

            switch (choice) {
                case 1:
                    System.out.print("Enter username part: ");
                    input = scanner.nextLine();
                    results = system.getUserManager().findByFilter(UserFilters.byUsernameContains(input));
                    break;
                case 2:
                    System.out.print("Enter email part: ");
                    input = scanner.nextLine();
                    results = system.getUserManager().findAll().stream()
                            .filter(u -> u.email().toLowerCase().contains(input.toLowerCase()))
                            .collect(Collectors.toList());
                    break;
                case 3:
                    System.out.print("Enter domain (e.g., @example.com): ");
                    input = scanner.nextLine();
                    results = system.getUserManager().findAll().stream()
                            .filter(u -> u.email().toLowerCase().endsWith(input.toLowerCase()))
                            .collect(Collectors.toList());
                    break;
                case 4:
                    System.out.print("Enter name part: ");
                    input = scanner.nextLine();
                    results = system.getUserManager().findByFilter(UserFilters.byFullNameContains(input));
                    break;
                default:
                    System.out.println("Invalid choice");
                    return;
            }

            System.out.println("\nSearch results:");
            for (User u : results) {
                System.out.printf("  %s - %s - %s%n", u.username(), u.fullName(), u.email());
            }
            System.out.printf("Found %d users%n", results.size());
        });
    }

    private void registerRoleCommands() {
        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            List<Role> roles = system.getRoleManager().findAll();
            System.out.println("\n========== ROLES ==========");
            System.out.printf("%-25s %-10s %-15s%n", "Name", "Permissions", "ID");
            System.out.println("-".repeat(55));
            for (Role role : roles) {
                System.out.printf("%-25s %-10d %-15s%n",
                        role.getName(), role.getPermissions().size(),
                        role.getId().substring(0, Math.min(8, role.getId().length())));
            }
            System.out.printf("Total: %d roles%n", roles.size());
        });

        parser.registerCommand("role-create", "Create new role", (scanner, system) -> {
            System.out.print("Role name: ");
            String name = scanner.nextLine();
            System.out.print("Description: ");
            String desc = scanner.nextLine();

            try {
                Role role = new Role(name, desc);
                system.getRoleManager().add(role);
                System.out.println("Role created successfully!");

                System.out.print("Add permissions? (yes/no): ");
                if (scanner.nextLine().equalsIgnoreCase("yes")) {
                    while (true) {
                        System.out.print("Permission name (or 'done' to finish): ");
                        String permName = scanner.nextLine();
                        if (permName.equalsIgnoreCase("done")) break;
                        System.out.print("Resource: ");
                        String resource = scanner.nextLine();
                        System.out.print("Description: ");
                        String permDesc = scanner.nextLine();

                        try {
                            Permission perm = new Permission(permName, resource, permDesc);
                            system.getRoleManager().addPermissionToRole(name, perm);
                            System.out.println("Permission added!");
                        } catch (Exception e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            System.out.print("Role name: ");
            String name = scanner.nextLine();

            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found");
                return;
            }

            System.out.println(roleOpt.get().format());
        });

        parser.registerCommand("role-update", "Update role name/description", (scanner, system) -> {
            System.out.print("Role name: ");
            String name = scanner.nextLine();

            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found");
                return;
            }

            Role role = roleOpt.get();
            System.out.print("New name (leave empty to keep): ");
            String newName = scanner.nextLine();
            System.out.print("New description (leave empty to keep): ");
            String newDesc = scanner.nextLine();

            if (!newName.isEmpty()) role.setName(newName);
            if (!newDesc.isEmpty()) role.setDescription(newDesc);
            System.out.println("Role updated!");
        });

        parser.registerCommand("role-delete", "Delete role", (scanner, system) -> {
            System.out.print("Role name: ");
            String name = scanner.nextLine();

            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found");
                return;
            }

            Role role = roleOpt.get();
            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
            if (!assignments.isEmpty()) {
                System.out.println("Warning: Role is assigned to users:");
                for (RoleAssignment ra : assignments) {
                    System.out.printf("  - %s%n", ra.user().username());
                }
            }

            System.out.print("Confirm delete (yes/no): ");
            if (!scanner.nextLine().equalsIgnoreCase("yes")) {
                System.out.println("Delete cancelled");
                return;
            }

            system.getRoleManager().remove(role);
            System.out.println("Role deleted!");
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            System.out.print("Role name: ");
            String roleName = scanner.nextLine();
            System.out.print("Permission name: ");
            String permName = scanner.nextLine();
            System.out.print("Resource: ");
            String resource = scanner.nextLine();
            System.out.print("Description: ");
            String desc = scanner.nextLine();

            try {
                Permission perm = new Permission(permName, resource, desc);
                system.getRoleManager().addPermissionToRole(roleName, perm);
                System.out.println("Permission added to role!");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            System.out.print("Role name: ");
            String roleName = scanner.nextLine();

            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found");
                return;
            }

            Role role = roleOpt.get();
            List<Permission> perms = new ArrayList<>(role.getPermissions());

            if (perms.isEmpty()) {
                System.out.println("Role has no permissions");
                return;
            }

            System.out.println("\nPermissions:");
            for (int i = 0; i < perms.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, perms.get(i).format());
            }

            System.out.print("Select permission to remove (number): ");
            int idx = Integer.parseInt(scanner.nextLine()) - 1;

            if (idx >= 0 && idx < perms.size()) {
                system.getRoleManager().removePermissionFromRole(roleName, perms.get(idx));
                System.out.println("Permission removed!");
            }
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            System.out.println("\nSearch filters:");
            System.out.println("1. By name (contains)");
            System.out.println("2. Has permission");
            System.out.println("3. Min permission count");
            System.out.print("Choose filter: ");

            int choice = Integer.parseInt(scanner.nextLine());
            List<Role> results = new ArrayList<>();

            switch (choice) {
                case 1:
                    System.out.print("Enter name part: ");
                    String name = scanner.nextLine();
                    results = system.getRoleManager().findByFilter(RoleFilters.byNameContains(name));
                    break;
                case 2:
                    System.out.print("Permission name: ");
                    String permName = scanner.nextLine();
                    System.out.print("Resource: ");
                    String resource = scanner.nextLine();
                    results = system.getRoleManager().findRolesWithPermission(permName, resource);
                    break;
                case 3:
                    System.out.print("Min number of permissions: ");
                    int min = Integer.parseInt(scanner.nextLine());
                    results = system.getRoleManager().findByFilter(RoleFilters.hasAtLeastNPermissions(min));
                    break;
                default:
                    System.out.println("Invalid choice");
                    return;
            }

            System.out.println("\nSearch results:");
            for (Role r : results) {
                System.out.printf("  %s - %d permissions%n", r.getName(), r.getPermissions().size());
            }
            System.out.printf("Found %d roles%n", results.size());
        });
    }

    private void registerAssignmentCommands() {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            User user = userOpt.get();
            List<Role> roles = system.getRoleManager().findAll();

            System.out.println("\nAvailable roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, roles.get(i).getName());
            }

            System.out.print("Select role (number): ");
            int roleIdx = Integer.parseInt(scanner.nextLine()) - 1;
            if (roleIdx < 0 || roleIdx >= roles.size()) {
                System.out.println("Invalid role");
                return;
            }

            Role role = roles.get(roleIdx);
            System.out.print("Assignment type (permanent/temporary): ");
            String type = scanner.nextLine();
            System.out.print("Reason: ");
            String reason = scanner.nextLine();

            AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

            try {
                if (type.equalsIgnoreCase("permanent")) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    system.getAssignmentManager().add(assignment);
                } else if (type.equalsIgnoreCase("temporary")) {
                    System.out.print("Expiration date (yyyy-MM-dd HH:mm:ss): ");
                    String expiresAt = scanner.nextLine();
                    System.out.print("Auto renew? (yes/no): ");
                    boolean autoRenew = scanner.nextLine().equalsIgnoreCase("yes");
                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    system.getAssignmentManager().add(assignment);
                } else {
                    System.out.println("Invalid type");
                    return;
                }
                System.out.println("Role assigned successfully!");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            User user = userOpt.get();
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            if (assignments.isEmpty()) {
                System.out.println("No active assignments for this user");
                return;
            }

            System.out.println("\nActive assignments:");
            for (int i = 0; i < assignments.size(); i++) {
                RoleAssignment ra = assignments.get(i);
                System.out.printf("%d. %s [%s]%n", i + 1, ra.role().getName(), ra.assignmentType());
            }

            System.out.print("Select assignment to revoke (number): ");
            int idx = Integer.parseInt(scanner.nextLine()) - 1;
            if (idx >= 0 && idx < assignments.size()) {
                system.getAssignmentManager().revokeAssignment(assignments.get(idx).assignmentId());
                System.out.println("Assignment revoked!");
            }
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) -> {
            List<RoleAssignment> assignments = system.getAssignmentManager().findAll();
            System.out.println("\n========== ASSIGNMENTS ==========");
            System.out.printf("%-20s %-15s %-10s %-10s %-20s%n", "User", "Role", "Type", "Status", "Assigned At");
            System.out.println("-".repeat(80));
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                String assignedAt = ra.metadata().assignedAt().length() > 19 ?
                        ra.metadata().assignedAt().substring(0, 19) : ra.metadata().assignedAt();
                System.out.printf("%-20s %-15s %-10s %-10s %-20s%n",
                        ra.user().username(), ra.role().getName(),
                        ra.assignmentType(), status, assignedAt);
            }
            System.out.printf("Total: %d assignments%n", assignments.size());
        });

        parser.registerCommand("assignment-list-user", "List user assignments", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get());
            System.out.println("\nAssignments for " + username + ":");
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  Role: %s | Type: %s | Status: %s | Assigned by: %s at %s%n",
                        ra.role().getName(),
                        ra.assignmentType(),
                        status,
                        ra.metadata().assignedBy(),
                        ra.metadata().assignedAt());
                if (ra.metadata().reason() != null && !ra.metadata().reason().isEmpty()) {
                    System.out.printf("    Reason: %s%n", ra.metadata().reason());
                }
                System.out.println();
            }
        });

        parser.registerCommand("assignment-active", "List active assignments", (scanner, system) -> {
            List<RoleAssignment> active = system.getAssignmentManager().getActiveAssignments();
            System.out.println("\nActive assignments:");
            for (RoleAssignment ra : active) {
                System.out.printf("  %s -> %s [%s]%n", ra.user().username(), ra.role().getName(), ra.assignmentType());
            }
            System.out.printf("Total: %d active assignments%n", active.size());
        });

        parser.registerCommand("assignment-expired", "List expired assignments", (scanner, system) -> {
            List<RoleAssignment> expired = system.getAssignmentManager().getExpiredAssignments();
            System.out.println("\nExpired assignments:");
            for (RoleAssignment ra : expired) {
                System.out.printf("  %s -> %s [%s]%n", ra.user().username(), ra.role().getName(), ra.assignmentType());
            }
            System.out.printf("Total: %d expired assignments%n", expired.size());
        });
    }

    private void registerPermissionCommands() {
        parser.registerCommand("permissions-user", "Show user permissions", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(userOpt.get());

            Map<String, List<Permission>> byResource = permissions.stream()
                    .collect(Collectors.groupingBy(Permission::resource));

            System.out.println("\nPermissions for " + username + ":");
            for (Map.Entry<String, List<Permission>> entry : byResource.entrySet()) {
                System.out.println("  " + entry.getKey() + ":");
                for (Permission p : entry.getValue()) {
                    System.out.printf("    - %s: %s%n", p.name(), p.description());
                }
            }
        });

        parser.registerCommand("permissions-check", "Check user permission", (scanner, system) -> {
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Permission name: ");
            String permName = scanner.nextLine();
            System.out.print("Resource: ");
            String resource = scanner.nextLine();

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found");
                return;
            }

            boolean has = system.getAssignmentManager().userHasPermission(userOpt.get(), permName, resource);
            System.out.println("User " + username + (has ? " HAS " : " DOES NOT HAVE ") +
                    permName + " on " + resource);
        });
    }

    private void registerUtilityCommands() {
        parser.registerCommand("help", "Show this help", (scanner, system) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "Show system statistics", (scanner, system) -> {
            System.out.println(system.generateStatistics());
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, system) -> {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit the program", (scanner, system) -> {
            System.out.print("Are you sure you want to exit? (yes/no): ");
            if (scanner.nextLine().equalsIgnoreCase("yes")) {
                System.out.println("Goodbye!");
                System.exit(0);
            }
        });
    }

    public CommandParser getParser() {
        return parser;
    }

    public void start() {
        System.out.println("RBAC System Started. Type 'help' for commands.");
        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            parser.parseAndExecute(input, scanner, system);
        }
    }
}