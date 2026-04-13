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
import src.utils.ConsoleUtils;
import src.utils.ReportGenerator;

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
        registerUserCommands();
        registerRoleCommands();
        registerAssignmentCommands();
        registerPermissionCommands();
        registerUtilityCommands();
    }

    private void registerUserCommands() {
        parser.registerCommand("user-list", "List all users", (scanner, system) -> {
            List<User> users = system.getUserManager().findAll();
            ConsoleUtils.printHeader("USERS");
            ConsoleUtils.printTableRow("Username", "Full Name", "Email");
            ConsoleUtils.printSeparator();
            for (User user : users) {
                System.out.printf("%-20s %-25s %-30s%n",
                        user.username(), user.fullName(), user.email());
            }
            ConsoleUtils.printSeparator();
            System.out.printf("Total: %d users%n", users.size());
        });

        parser.registerCommand("user-create", "Create new user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            String fullName = ConsoleUtils.promptString(scanner, "Full name: ", true);
            String email = ConsoleUtils.promptString(scanner, "Email: ", true);

            try {
                User user = User.validate(username, fullName, email);
                system.getUserManager().add(user);
                system.getAuditLog().log("USER_CREATE", system.getCurrentUser(), username,
                        "Full name: " + fullName + ", Email: " + email);
                ConsoleUtils.printSuccess("User created successfully!");
            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user details", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            User user = userOpt.get();
            ConsoleUtils.printHeader("USER DETAILS: " + user.username());
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
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            if (!system.getUserManager().exists(username)) {
                ConsoleUtils.printError("User not found");
                return;
            }

            String fullName = ConsoleUtils.promptString(scanner, "New full name (leave empty to keep): ", false);
            String email = ConsoleUtils.promptString(scanner, "New email (leave empty to keep): ", false);

            try {
                system.getUserManager().update(username,
                        fullName.isEmpty() ? null : fullName,
                        email.isEmpty() ? null : email);
                system.getAuditLog().log("USER_UPDATE", system.getCurrentUser(), username,
                        "New full name: " + fullName + ", New email: " + email);
                ConsoleUtils.printSuccess("User updated successfully!");
            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            User user = userOpt.get();

            if (!ConsoleUtils.promptYesNo(scanner, "Are you sure you want to delete user '" + username + "'?")) {
                ConsoleUtils.printInfo("Delete cancelled");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
            for (RoleAssignment ra : assignments) {
                system.getAssignmentManager().remove(ra);
            }

            system.getUserManager().remove(user);
            system.getAuditLog().log("USER_DELETE", system.getCurrentUser(), username,
                    "Deleted user with " + assignments.size() + " assignments");
            ConsoleUtils.printSuccess("User deleted successfully!");
        });

        parser.registerCommand("user-search", "Search users by filter", (scanner, system) -> {
            List<String> filters = List.of(
                    "By username (contains)",
                    "By email (contains)",
                    "By email domain",
                    "By full name (contains)"
            );

            String choice = ConsoleUtils.promptChoice(scanner, "Search filters:", filters).toString();
            String input;
            List<User> results = new ArrayList<>();

            if (choice.startsWith("By username")) {
                input = ConsoleUtils.promptString(scanner, "Enter username part: ", true);
                results = system.getUserManager().findByFilter(UserFilters.byUsernameContains(input));
            } else if (choice.startsWith("By email (contains)")) {
                input = ConsoleUtils.promptString(scanner, "Enter email part: ", true);
                results = system.getUserManager().findAll().stream()
                        .filter(u -> u.email().toLowerCase().contains(input.toLowerCase()))
                        .collect(Collectors.toList());
            } else if (choice.startsWith("By email domain")) {
                input = ConsoleUtils.promptString(scanner, "Enter domain (e.g., @example.com): ", true);
                results = system.getUserManager().findAll().stream()
                        .filter(u -> u.email().toLowerCase().endsWith(input.toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                input = ConsoleUtils.promptString(scanner, "Enter name part: ", true);
                results = system.getUserManager().findByFilter(UserFilters.byFullNameContains(input));
            }

            ConsoleUtils.printHeader("SEARCH RESULTS");
            for (User u : results) {
                System.out.printf("  %s - %s - %s%n", u.username(), u.fullName(), u.email());
            }
            System.out.printf("Found %d users%n", results.size());
        });
    }

    private void registerRoleCommands() {
        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            List<Role> roles = system.getRoleManager().findAll();
            ConsoleUtils.printHeader("ROLES");
            ConsoleUtils.printTableRow("Name", "Permissions", "ID");
            ConsoleUtils.printSeparator();
            for (Role role : roles) {
                System.out.printf("%-25s %-10d %-15s%n",
                        role.getName(), role.getPermissions().size(),
                        role.getId().substring(0, Math.min(8, role.getId().length())));
            }
            ConsoleUtils.printSeparator();
            System.out.printf("Total: %d roles%n", roles.size());
        });

        parser.registerCommand("role-create", "Create new role", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);
            String desc = ConsoleUtils.promptString(scanner, "Description: ", true);

            try {
                Role role = new Role(name, desc);
                system.getRoleManager().add(role);
                system.getAuditLog().log("ROLE_CREATE", system.getCurrentUser(), name,
                        "Description: " + desc);
                ConsoleUtils.printSuccess("Role created successfully!");

                if (ConsoleUtils.promptYesNo(scanner, "Add permissions?")) {
                    while (true) {
                        String permName = ConsoleUtils.promptString(scanner, "Permission name (or 'done' to finish): ", false);
                        if (permName.equalsIgnoreCase("done")) break;

                        String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);
                        String permDesc = ConsoleUtils.promptString(scanner, "Description: ", true);

                        try {
                            Permission perm = new Permission(permName, resource, permDesc);
                            system.getRoleManager().addPermissionToRole(name, perm);
                            system.getAuditLog().log("PERMISSION_ADD", system.getCurrentUser(), name,
                                    "Permission: " + permName + " on " + resource);
                            ConsoleUtils.printSuccess("Permission added!");
                        } catch (Exception e) {
                            ConsoleUtils.printError(e.getMessage());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);

            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }

            System.out.println(roleOpt.get().format());
        });

        parser.registerCommand("role-update", "Update role name/description", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);

            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }

            Role role = roleOpt.get();
            String newName = ConsoleUtils.promptString(scanner, "New name (leave empty to keep): ", false);
            String newDesc = ConsoleUtils.promptString(scanner, "New description (leave empty to keep): ", false);

            if (!newName.isEmpty()) role.setName(newName);
            if (!newDesc.isEmpty()) role.setDescription(newDesc);

            system.getAuditLog().log("ROLE_UPDATE", system.getCurrentUser(), name,
                    "New name: " + newName + ", New description: " + newDesc);
            ConsoleUtils.printSuccess("Role updated!");
        });

        parser.registerCommand("role-delete", "Delete role", (scanner, system) -> {
            String name = ConsoleUtils.promptString(scanner, "Role name: ", true);

            Optional<Role> roleOpt = system.getRoleManager().findByName(name);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }

            Role role = roleOpt.get();
            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);

            if (!assignments.isEmpty()) {
                ConsoleUtils.printInfo("Warning: Role is assigned to users:");
                for (RoleAssignment ra : assignments) {
                    System.out.printf("  - %s%n", ra.user().username());
                }
            }

            if (!ConsoleUtils.promptYesNo(scanner, "Confirm delete?")) {
                ConsoleUtils.printInfo("Delete cancelled");
                return;
            }

            system.getRoleManager().remove(role);
            system.getAuditLog().log("ROLE_DELETE", system.getCurrentUser(), name,
                    "Deleted role with " + assignments.size() + " assignments");
            ConsoleUtils.printSuccess("Role deleted!");
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name: ", true);
            String permName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
            String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);
            String desc = ConsoleUtils.promptString(scanner, "Description: ", true);

            try {
                Permission perm = new Permission(permName, resource, desc);
                system.getRoleManager().addPermissionToRole(roleName, perm);
                system.getAuditLog().log("PERMISSION_ADD", system.getCurrentUser(), roleName,
                        "Permission: " + permName + " on " + resource);
                ConsoleUtils.printSuccess("Permission added to role!");
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Role name: ", true);

            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                ConsoleUtils.printError("Role not found");
                return;
            }

            Role role = roleOpt.get();
            List<Permission> perms = new ArrayList<>(role.getPermissions());

            if (perms.isEmpty()) {
                ConsoleUtils.printError("Role has no permissions");
                return;
            }

            Permission selected = ConsoleUtils.promptChoice(scanner, "Select permission to remove:", perms);

            system.getRoleManager().removePermissionFromRole(roleName, selected);
            system.getAuditLog().log("PERMISSION_REMOVE", system.getCurrentUser(), roleName,
                    "Permission: " + selected.name() + " on " + selected.resource());
            ConsoleUtils.printSuccess("Permission removed!");
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            List<String> filters = List.of(
                    "By name (contains)",
                    "Has permission",
                    "Min permission count"
            );

            String choice = ConsoleUtils.promptChoice(scanner, "Search filters:", filters).toString();
            List<Role> results = new ArrayList<>();

            if (choice.startsWith("By name")) {
                String name = ConsoleUtils.promptString(scanner, "Enter name part: ", true);
                results = system.getRoleManager().findByFilter(RoleFilters.byNameContains(name));
            } else if (choice.startsWith("Has permission")) {
                String permName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
                String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);
                results = system.getRoleManager().findRolesWithPermission(permName, resource);
            } else {
                int min = ConsoleUtils.promptInt(scanner, "Min number of permissions: ", 0, 100);
                results = system.getRoleManager().findByFilter(RoleFilters.hasAtLeastNPermissions(min));
            }

            ConsoleUtils.printHeader("SEARCH RESULTS");
            for (Role r : results) {
                System.out.printf("  %s - %d permissions%n", r.getName(), r.getPermissions().size());
            }
            System.out.printf("Found %d roles%n", results.size());
        });
    }

    private void registerAssignmentCommands() {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            User user = userOpt.get();
            List<Role> roles = system.getRoleManager().findAll();

            Role role = ConsoleUtils.promptChoice(scanner, "Select role:", roles);

            String type = ConsoleUtils.promptChoice(scanner, "Assignment type:",
                    List.of("permanent", "temporary")).toString();

            String reason = ConsoleUtils.promptString(scanner, "Reason: ", false);

            AssignmentMetadata metadata = AssignmentMetadata.now(system.getCurrentUser(), reason);

            try {
                if (type.equals("permanent")) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    system.getAssignmentManager().add(assignment);
                    system.getAuditLog().log("ROLE_ASSIGN", system.getCurrentUser(), username,
                            "Role: " + role.getName() + ", Type: permanent, Reason: " + reason);
                    ConsoleUtils.printSuccess("Role assigned successfully!");
                } else {
                    String expiresAt = ConsoleUtils.promptDate(scanner, "Expiration date");
                    boolean autoRenew = ConsoleUtils.promptYesNo(scanner, "Auto renew?");
                    TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                    system.getAssignmentManager().add(assignment);
                    system.getAuditLog().log("ROLE_ASSIGN", system.getCurrentUser(), username,
                            "Role: " + role.getName() + ", Type: temporary, Expires: " + expiresAt + ", Reason: " + reason);
                    ConsoleUtils.printSuccess("Role assigned successfully!");
                }
            } catch (Exception e) {
                ConsoleUtils.printError(e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            User user = userOpt.get();
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());

            if (assignments.isEmpty()) {
                ConsoleUtils.printError("No active assignments for this user");
                return;
            }

            RoleAssignment selected = ConsoleUtils.promptChoice(scanner, "Select assignment to revoke:", assignments);

            system.getAssignmentManager().revokeAssignment(selected.assignmentId());
            system.getAuditLog().log("ROLE_REVOKE", system.getCurrentUser(), username,
                    "Role: " + selected.role().getName());
            ConsoleUtils.printSuccess("Assignment revoked!");
        });

        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) -> {
            List<RoleAssignment> assignments = system.getAssignmentManager().findAll();
            ConsoleUtils.printHeader("ASSIGNMENTS");
            ConsoleUtils.printTableRow("User", "Role", "Type", "Status", "Assigned At");
            ConsoleUtils.printSeparator();
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                String assignedAt = ra.metadata().assignedAt().length() > 19 ?
                        ra.metadata().assignedAt().substring(0, 19) : ra.metadata().assignedAt();
                System.out.printf("%-20s %-15s %-10s %-10s %-20s%n",
                        ra.user().username(), ra.role().getName(),
                        ra.assignmentType(), status, assignedAt);
            }
            ConsoleUtils.printSeparator();
            System.out.printf("Total: %d assignments%n", assignments.size());
        });

        parser.registerCommand("assignment-list-user", "List user assignments", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get());
            ConsoleUtils.printHeader("ASSIGNMENTS FOR " + username);
            for (RoleAssignment ra : assignments) {
                String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  Role: %s | Type: %s | Status: %s | Assigned by: %s at %s%n",
                        ra.role().getName(), ra.assignmentType(), status,
                        ra.metadata().assignedBy(), ra.metadata().assignedAt());
                if (ra.metadata().reason() != null && !ra.metadata().reason().isEmpty()) {
                    System.out.printf("    Reason: %s%n", ra.metadata().reason());
                }
                System.out.println();
            }
        });

        parser.registerCommand("assignment-active", "List active assignments", (scanner, system) -> {
            List<RoleAssignment> active = system.getAssignmentManager().getActiveAssignments();
            ConsoleUtils.printHeader("ACTIVE ASSIGNMENTS");
            for (RoleAssignment ra : active) {
                System.out.printf("  %s -> %s [%s]%n", ra.user().username(), ra.role().getName(), ra.assignmentType());
            }
            System.out.printf("Total: %d active assignments%n", active.size());
        });

        parser.registerCommand("assignment-expired", "List expired assignments", (scanner, system) -> {
            List<RoleAssignment> expired = system.getAssignmentManager().getExpiredAssignments();
            ConsoleUtils.printHeader("EXPIRED ASSIGNMENTS");
            for (RoleAssignment ra : expired) {
                System.out.printf("  %s -> %s [%s]%n", ra.user().username(), ra.role().getName(), ra.assignmentType());
            }
            System.out.printf("Total: %d expired assignments%n", expired.size());
        });
    }

    private void registerPermissionCommands() {
        parser.registerCommand("permissions-user", "Show user permissions", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(userOpt.get());

            Map<String, List<Permission>> byResource = permissions.stream()
                    .collect(Collectors.groupingBy(Permission::resource));

            ConsoleUtils.printHeader("PERMISSIONS FOR " + username);
            for (Map.Entry<String, List<Permission>> entry : byResource.entrySet()) {
                System.out.println("  " + entry.getKey() + ":");
                for (Permission p : entry.getValue()) {
                    System.out.printf("    - %s: %s%n", p.name(), p.description());
                }
            }
        });

        parser.registerCommand("permissions-check", "Check user permission", (scanner, system) -> {
            String username = ConsoleUtils.promptString(scanner, "Username: ", true);
            String permName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
            String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);

            Optional<User> userOpt = system.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                ConsoleUtils.printError("User not found");
                return;
            }

            boolean has = system.getAssignmentManager().userHasPermission(userOpt.get(), permName, resource);
            if (has) {
                ConsoleUtils.printSuccess("User " + username + " HAS " + permName + " on " + resource);
            } else {
                ConsoleUtils.printError("User " + username + " DOES NOT HAVE " + permName + " on " + resource);
            }
        });
    }

    private void registerUtilityCommands() {
        parser.registerCommand("help", "Show this help", (scanner, system) -> {
            parser.printHelp();
        });

        parser.registerCommand("stats", "Show system statistics", (scanner, system) -> {
            System.out.println(system.generateStatistics());
        });

        parser.registerCommand("audit-log", "Show audit log", (scanner, system) -> {
            system.getAuditLog().printLog();
        });

        parser.registerCommand("report-users", "Generate user report", (scanner, system) -> {
            String report = ReportGenerator.generateUserReport(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );
            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Save to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Filename: ", true);
                ReportGenerator.exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-roles", "Generate role report", (scanner, system) -> {
            String report = ReportGenerator.generateRoleReport(
                    system.getRoleManager(),
                    system.getAssignmentManager()
            );
            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Save to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Filename: ", true);
                ReportGenerator.exportToFile(report, filename);
            }
        });

        parser.registerCommand("report-matrix", "Generate permission matrix", (scanner, system) -> {
            String report = ReportGenerator.generatePermissionMatrix(
                    system.getUserManager(),
                    system.getAssignmentManager()
            );
            System.out.println(report);

            if (ConsoleUtils.promptYesNo(scanner, "Save to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Filename: ", true);
                ReportGenerator.exportToFile(report, filename);
            }
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, system) -> {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit the program", (scanner, system) -> {
            if (ConsoleUtils.promptYesNo(scanner, "Are you sure you want to exit?")) {
                System.out.println("Goodbye!");
                System.exit(0);
            }
        });
    }

    public CommandParser getParser() {
        return parser;
    }

    public void start() {
        ConsoleUtils.printHeader("RBAC SYSTEM STARTED");
        System.out.println("Type 'help' for available commands.\n");
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            parser.parseAndExecute(input, scanner, system);
        }
    }
}