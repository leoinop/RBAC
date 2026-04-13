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
        sb.append(FormatUtils.formatHeader("USER REPORT"));
        sb.append("\n\n");

        List<User> users = userManager.findAll();

        for (User user : users) {
            sb.append(FormatUtils.formatBox("User: " + user.username())).append("\n");
            sb.append(FormatUtils.formatKeyValue("Full Name", user.fullName())).append("\n");
            sb.append(FormatUtils.formatKeyValue("Email", user.email())).append("\n");

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

        sb.append(FormatUtils.formatSeparator()).append("\n");
        sb.append(String.format("Total users: %d\n", users.size()));
        return sb.toString();
    }

    public static String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("ROLE REPORT"));
        sb.append("\n\n");

        List<Role> roles = roleManager.findAll();

        String[] headers = {"Role", "Permissions", "Users", "Active"};
        List<String[]> rows = new ArrayList<>();

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeCount = assignments.stream().filter(RoleAssignment::isActive).count();

            rows.add(new String[]{
                    role.getName(),
                    String.valueOf(role.getPermissions().size()),
                    String.valueOf(assignments.size()),
                    String.valueOf(activeCount)
            });
        }

        sb.append(FormatUtils.formatTable(headers, rows)).append("\n\n");

        for (Role role : roles) {
            sb.append(FormatUtils.formatBox("Role: " + role.getName())).append("\n");
            sb.append(FormatUtils.formatKeyValue("Description", role.getDescription())).append("\n");

            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            if (!assignments.isEmpty()) {
                sb.append("Users with this role:\n");
                for (RoleAssignment ra : assignments) {
                    String status = ra.isActive() ? "ACTIVE" : "INACTIVE";
                    sb.append(String.format("  - %s [%s]\n", ra.user().username(), status));
                }
            }
            sb.append("\n");
        }

        sb.append(FormatUtils.formatSeparator()).append("\n");
        sb.append(String.format("Total roles: %d\n", roles.size()));
        return sb.toString();
    }

    public static String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("PERMISSION MATRIX"));
        sb.append("\n\n");

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

        String[] headers = new String[sortedResources.size() + 1];
        headers[0] = "User";
        for (int i = 0; i < sortedResources.size(); i++) {
            headers[i + 1] = sortedResources.get(i);
        }

        List<String[]> rows = new ArrayList<>();
        for (User user : users) {
            String[] row = new String[sortedResources.size() + 1];
            row[0] = user.username();

            for (int i = 0; i < sortedResources.size(); i++) {
                String resource = sortedResources.get(i);
                Set<String> perms = userPermissions.get(user.username());
                if (perms != null) {
                    List<String> userResourcePerms = perms.stream()
                            .filter(p -> p.endsWith(":" + resource))
                            .map(p -> p.split(":")[0])
                            .collect(Collectors.toList());
                    row[i + 1] = userResourcePerms.isEmpty() ? "-" : String.join(",", userResourcePerms);
                } else {
                    row[i + 1] = "-";
                }
            }
            rows.add(row);
        }

        sb.append(FormatUtils.formatTable(headers, rows));
        sb.append("\n\nLegend: READ, WRITE, DELETE, MANAGE, etc.\n");
        return sb.toString();
    }

    public static String generateCompactUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        String[] headers = {"Username", "Full Name", "Email", "Roles"};
        List<String[]> rows = new ArrayList<>();

        for (User user : users) {
            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            String rolesStr = assignments.stream()
                    .map(ra -> ra.role().getName())
                    .collect(Collectors.joining(", "));
            if (rolesStr.isEmpty()) {
                rolesStr = "-";
            }

            rows.add(new String[]{
                    user.username(),
                    FormatUtils.truncate(user.fullName(), 20),
                    user.email(),
                    FormatUtils.truncate(rolesStr, 30)
            });
        }

        sb.append(FormatUtils.formatTable(headers, rows));
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