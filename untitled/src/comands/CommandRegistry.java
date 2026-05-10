package commands;

import system.RBACSystem;
import system.BackgroundExecutor;
import models.User;
import models.Role;
import models.Permission;
import models.RoleAssignment;
import models.PermanentAssignment;
import models.TemporaryAssignment;
import models.AssignmentMetadata;
import filters.UserFilters;
import filters.RoleFilters;
import utils.ConsoleUtils;
import utils.ReportGenerator;
import utils.DateUtils;
import utils.FormatUtils;

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
        registerAsyncCommands();
    }

    private void registerAsyncCommands() {
        parser.registerCommand("report-users-async", "Generate user report in background", (scanner, system) -> {
            ConsoleUtils.printInfo("Starting background user report generation...");

            system.getBackgroundExecutor().submit(() -> {
                try {
                    String report = ReportGenerator.generateUserReportParallel(
                            system.getUserManager(),
                            system.getAssignmentManager()
                    );

                    System.out.println("\n[Background] Report generated:");
                    System.out.println(report);

                    system.getAuditLog().log("REPORT_GENERATED", system.getCurrentUser(), "users",
                            "Background user report completed");
                } catch (Exception e) {
                    System.err.println("[Background] Error generating report: " + e.getMessage());
                }
            });

            ConsoleUtils.printSuccess("Background report generation started. Type 'audit-log' to see completion.");
        });

        parser.registerCommand("report-matrix-async", "Generate permission matrix in background", (scanner, system) -> {
            ConsoleUtils.printInfo("Starting background permission matrix generation...");

            system.getBackgroundExecutor().submit(() -> {
                try {
                    String report = ReportGenerator.generatePermissionMatrixParallel(
                            system.getUserManager(),
                            system.getAssignmentManager()
                    );

                    System.out.println("\n[Background] Permission matrix generated:");
                    System.out.println(report);

                    system.getAuditLog().log("REPORT_GENERATED", system.getCurrentUser(), "matrix",
                            "Background permission matrix completed");
                } catch (Exception e) {
                    System.err.println("[Background] Error generating matrix: " + e.getMessage());
                }
            });

            ConsoleUtils.printSuccess("Background matrix generation started.");
        });

        parser.registerCommand("save-async", "Save data to file in background", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Filename to save: ", true);

            ConsoleUtils.printInfo("Starting background save to " + filename);

            system.getBackgroundExecutor().submit(() -> {
                try {
                    List<User> users = system.getUserManager().findAll();
                    List<Role> roles = system.getRoleManager().findAll();
                    List<RoleAssignment> assignments = system.getAssignmentManager().findAll();

                    StringBuilder sb = new StringBuilder();
                    sb.append("=== RBAC DATA BACKUP ===\n");
                    sb.append("Generated: ").append(DateUtils.getCurrentDateTime()).append("\n\n");

                    sb.append("--- USERS ---\n");
                    for (User u : users) {
                        sb.append(u.username()).append("|").append(u.fullName()).append("|").append(u.email()).append("\n");
                    }

                    sb.append("\n--- ROLES ---\n");
                    for (Role r : roles) {
                        sb.append(r.getName()).append("|").append(r.getDescription()).append("\n");
                    }

                    sb.append("\n--- ASSIGNMENTS ---\n");
                    for (RoleAssignment ra : assignments) {
                        sb.append(ra.user().username()).append("|")
                                .append(ra.role().getName()).append("|")
                                .append(ra.assignmentType()).append("|")
                                .append(ra.isActive()).append("\n");
                    }

                    try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(filename))) {
                        writer.write(sb.toString());
                    }

                    system.getAuditLog().log("DATA_SAVED", system.getCurrentUser(), "system",
                            "Background save to " + filename);

                    System.out.println("[Background] Data saved to " + filename);
                } catch (Exception e) {
                    System.err.println("[Background] Error saving data: " + e.getMessage());
                }
            });

            ConsoleUtils.printSuccess("Background save started.");
        });

        parser.registerCommand("load-async", "Load data from file in background", (scanner, system) -> {
            String filename = ConsoleUtils.promptString(scanner, "Filename to load: ", true);

            ConsoleUtils.printInfo("Starting background load from " + filename);

            system.getBackgroundExecutor().submit(() -> {
                try {
                    java.io.File file = new java.io.File(filename);
                    if (!file.exists()) {
                        System.err.println("[Background] File not found: " + filename);
                        return;
                    }

                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                        String line;
                        String section = "";
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("---")) {
                                section = line.replace("-", "").trim();
                                continue;
                            }
                            if (line.isEmpty() || line.startsWith("===")) continue;

                            if (section.equals("USERS")) {
                                String[] parts = line.split("\\|");
                                if (parts.length >= 3) {
                                    try {
                                        User user = User.validate(parts[0], parts[1], parts[2]);
                                        system.getUserManager().add(user);
                                    } catch (IllegalArgumentException e) {
                                        // User might already exist
                                    }
                                }
                            } else if (section.equals("ROLES")) {
                                String[] parts = line.split("\\|");
                                if (parts.length >= 2) {
                                    try {
                                        Role role = new Role(parts[0], parts[1]);
                                        system.getRoleManager().add(role);
                                    } catch (IllegalArgumentException e) {
                                        // Role might already exist
                                    }
                                }
                            }
                        }
                    }

                    system.getAuditLog().log("DATA_LOADED", system.getCurrentUser(), "system",
                            "Background load from " + filename);

                    System.out.println("[Background] Data loaded from " + filename);
                } catch (Exception e) {
                    System.err.println("[Background] Error loading data: " + e.getMessage());
                }
            });

            ConsoleUtils.printSuccess("Background load started.");
        });

        parser.registerCommand("tasks-status", "Show background tasks status", (scanner, system) -> {
            ConsoleUtils.printHeader("BACKGROUND TASKS STATUS");
            System.out.println("Executor shutdown: " + system.getBackgroundExecutor().isShutdown());
            System.out.println("Background executor is active and ready");
            ConsoleUtils.printInfo("Use report-users-async, report-matrix-async, save-async, load-async for background operations");
        });
    }

    private void registerUserCommands() {
        parser.registerCommand("user-list", "List all users", (scanner, system) -> {
            List<User> users = system.getUserManager().findAll();

            String[] headers = {"Username", "Full Name", "Email"};
            List<String[]> rows = new ArrayList<>();

            for (User user : users) {
                rows.add(new String[]{
                        user.username(),
                        FormatUtils.truncate(user.fullName(), 25),
                        user.email()
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Total: " + users.size() + " users");
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
                results = system.getUserManager().findByFilterParallel(UserFilters.byUsernameContains(input));
            } else if (choice.startsWith("By email (contains)")) {
                input = ConsoleUtils.promptString(scanner, "Enter email part: ", true);
                results = system.getUserManager().findAll().parallelStream()
                        .filter(u -> u.email().toLowerCase().contains(input.toLowerCase()))
                        .collect(Collectors.toList());
            } else if (choice.startsWith("By email domain")) {
                input = ConsoleUtils.promptString(scanner, "Enter domain (e.g., @example.com): ", true);
                results = system.getUserManager().findAll().parallelStream()
                        .filter(u -> u.email().toLowerCase().endsWith(input.toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                input = ConsoleUtils.promptString(scanner, "Enter name part: ", true);
                results = system.getUserManager().findByFilterParallel(UserFilters.byFullNameContains(input));
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

            String[] headers = {"Name", "Permissions", "ID"};
            List<String[]> rows = new ArrayList<>();

            for (Role role : roles) {
                rows.add(new String[]{
                        role.getName(),
                        String.valueOf(role.getPermissions().size()),
                        FormatUtils.truncate(role.getId(), 12)
                });
            }

            System.out.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Total: " + roles.size() + " roles");
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
                results = system.getRoleManager().findByFilterParallel(RoleFilters.byNameContains(name));
            } else if (choice.startsWith("Has permission")) {
                String permName = ConsoleUtils.promptString(scanner, "Permission name: ", true);
                String resource = ConsoleUtils.promptString(scanner, "Resource: ", true);
                results = system.getRoleManager().findRolesWithPermission(permName, resource);
            } else {
                int min = ConsoleUtils.promptInt(scanner, "Min number of permissions: ", 0, 100);
                results = system.getRoleManager().findByFilterParallel(RoleFilters.hasAtLeastNPermissions(min));
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

                    String warning = DateUtils.getExpirationWarning(expiresAt);
                    if (warning.contains("WARNING") || warning.equals("EXPIRES TODAY")) {
                        ConsoleUtils.printError(warning);
                        if (!ConsoleUtils.promptYesNo(scanner, "Continue anyway?")) {
                            return;
                        }
                    } else {
                        ConsoleUtils.printInfo(warning);
                    }

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
                System.out.printf("  Role: %s | Type: %s | Status: %s%n",
                        ra.role().getName(), ra.assignmentType(), status);
                System.out.printf("  Assigned by: %s at %s%n",
                        ra.metadata().assignedBy(), ra.metadata().assignedAt());

                if (ra instanceof TemporaryAssignment temp) {
                    String relativeTime = DateUtils.formatRelativeTime(temp.getExpiresAt());
                    String warning = DateUtils.getExpirationWarning(temp.getExpiresAt());
                    System.out.printf("  Expires: %s (%s)%n", temp.getExpiresAt(), relativeTime);
                    if (warning.contains("WARNING") || warning.equals("EXPIRED")) {
                        ConsoleUtils.printError("  " + warning);
                    } else {
                        System.out.printf("  %s%n", warning);
                    }
                }

                if (ra.metadata().reason() != null && !ra.metadata().reason().isEmpty()) {
                    System.out.printf("  Reason: %s%n", ra.metadata().reason());
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