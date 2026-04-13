package src.utils;

import src.managers.UserManager;
import src.managers.RoleManager;
import src.managers.AssignmentManager;
import models.User;
import models.Role;
import models.RoleAssignment;
import models.Permission;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    private ReportGenerator() {
    }

    public static String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== USER REPORT ==========\n\n");

        List<User> users = userManager.findAll();

        for (User user : users) {
            sb.append(String.format("User: %s (%s)\n", user.username(), user.fullName()));
            sb.append(String.format("Email: %s\n", user.email()));

            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            if (assignments.isEmpty()) {
                sb.append("Roles: No roles assigned\n");
            } else {
                sb.append("Roles:\n");
                for (RoleAssignment ra : assignments) {
                    String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                    sb.append(String.format("  - %s [%s] - %s\n",
                            ra.role().getName(), ra.assignmentType(), status));
                }
            }
            sb.append("\n");
        }

        sb.append(String.format("Total users: %d\n", users.size()));
        sb.append("==================================\n");
        return sb.toString();
    }

    public static String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== ROLE REPORT ==========\n\n");

        List<Role> roles = roleManager.findAll();

        for (Role role : roles) {
            sb.append(String.format("Role: %s\n", role.getName()));
            sb.append(String.format("Description: %s\n", role.getDescription()));
            sb.append(String.format("Permissions: %d\n", role.getPermissions().size()));

            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeCount = assignments.stream().filter(RoleAssignment::isActive).count();

            sb.append(String.format("Assigned to: %d users (%d active)\n", assignments.size(), activeCount));

            if (!assignments.isEmpty()) {
                sb.append("Users with this role:\n");
                for (RoleAssignment ra : assignments) {
                    String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                    sb.append(String.format("  - %s [%s]\n", ra.user().username(), status));
                }
            }
            sb.append("\n");
        }

        sb.append(String.format("Total roles: %d\n", roles.size()));
        sb.append("=================================\n");
        return sb.toString();
    }

    public static String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== PERMISSION MATRIX ==========\n\n");

        List<User> users = userManager.findAll();

        Set<String> allResources = new HashSet<>();
        Map<String, Set<String>> userPermissions = new HashMap<>();

        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            Set<String> permStrings = new HashSet<>();
            for (Permission p : perms) {
                String key = p.name() + ":" + p.resource();
                permStrings.add(key);
                allResources.add(p.resource());
            }
            userPermissions.put(user.username(), permStrings);
        }

        List<String> sortedResources = new ArrayList<>(allResources);
        Collections.sort(sortedResources);

        sb.append(String.format("%-20s", "User"));
        for (String resource : sortedResources) {
            sb.append(String.format(" | %-15s", resource));
        }
        sb.append("\n");

        for (User user : users) {
            sb.append(String.format("%-20s", user.username()));

            for (String resource : sortedResources) {
                Set<String> perms = userPermissions.get(user.username());
                if (perms != null) {
                    List<String> userResourcePerms = perms.stream()
                            .filter(p -> p.endsWith(":" + resource))
                            .map(p -> p.split(":")[0])
                            .collect(Collectors.toList());

                    if (userResourcePerms.isEmpty()) {
                        sb.append(String.format(" | %-15s", "-"));
                    } else {
                        sb.append(String.format(" | %-15s", String.join(",", userResourcePerms)));
                    }
                } else {
                    sb.append(String.format(" | %-15s", "-"));
                }
            }
            sb.append("\n");
        }

        sb.append("\nLegend: READ, WRITE, DELETE, MANAGE, etc.\n");
        sb.append("========================================\n");
        return sb.toString();
    }

    public static void exportToFile(String report, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(report);
            System.out.println("Report saved to: " + filename);
        } catch (IOException e) {
            System.out.println("Error saving report: " + e.getMessage());
        }
    }
}