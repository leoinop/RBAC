package system;

import managers.*;
import models.*;
import utils.AuditLog;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final BackgroundExecutor backgroundExecutor;

    private final ScheduledTasks scheduledTasks;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.assignmentManager = new AssignmentManager(userManager, null);
        this.roleManager = new RoleManager(assignmentManager);
        this.auditLog = new AuditLog();
        this.backgroundExecutor = new BackgroundExecutor();

        this.scheduledTasks = new ScheduledTasks(this);

        this.currentUser = "system";
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public BackgroundExecutor getBackgroundExecutor() {
        return backgroundExecutor;
    }


    public ScheduledTasks getScheduledTasks() {
        return scheduledTasks;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        // Создаем права доступа
        Permission readUsers = new Permission("READ", "users", "Can view users");
        Permission writeUsers = new Permission("WRITE", "users", "Can create and edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");
        Permission readReports = new Permission("READ", "reports", "Can view reports");
        Permission writeReports = new Permission("WRITE", "reports", "Can create and edit reports");
        Permission deleteReports = new Permission("DELETE", "reports", "Can delete reports");
        Permission readSettings = new Permission("READ", "settings", "Can view settings");
        Permission writeSettings = new Permission("WRITE", "settings", "Can edit settings");
        Permission manageRoles = new Permission("MANAGE", "roles", "Can manage roles");

        // Создаем роли
        Role adminRole = new Role("Admin", "Full system access");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readReports);
        adminRole.addPermission(writeReports);
        adminRole.addPermission(deleteReports);
        adminRole.addPermission(readSettings);
        adminRole.addPermission(writeSettings);
        adminRole.addPermission(manageRoles);

        Role managerRole = new Role("Manager", "Can manage users and view reports");
        managerRole.addPermission(readUsers);
        managerRole.addPermission(writeUsers);
        managerRole.addPermission(readReports);
        managerRole.addPermission(writeReports);

        Role viewerRole = new Role("Viewer", "Can only view data");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readReports);

        // Добавляем роли в менеджер
        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        // Создаем тестового администратора
        User admin = User.validate("admin", "System Administrator", "admin@rbac.com");
        userManager.add(admin);

        // Назначаем роль Admin администратору
        AssignmentMetadata metadata = AssignmentMetadata.now("system", "Initial admin setup");
        PermanentAssignment assignment = new PermanentAssignment(admin, adminRole, metadata);
        assignmentManager.add(assignment);

        // Создаем тестового пользователя
        User testUser = User.validate("test_user", "Test User", "test@example.com");
        userManager.add(testUser);

        // Назначаем роль Viewer тестовому пользователю
        AssignmentMetadata viewerMetadata = AssignmentMetadata.now("system", "Test user setup");
        PermanentAssignment viewerAssignment = new PermanentAssignment(testUser, viewerRole, viewerMetadata);
        assignmentManager.add(viewerAssignment);
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== RBAC SYSTEM STATISTICS ==========\n");
        sb.append(String.format("Users: %d\n", userManager.count()));
        sb.append(String.format("Roles: %d\n", roleManager.count()));
        sb.append(String.format("Assignments: %d\n", assignmentManager.count()));

        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long expiredAssignments = assignmentManager.getExpiredAssignments().size();
        sb.append(String.format("Active assignments: %d\n", activeAssignments));
        sb.append(String.format("Expired assignments: %d\n", expiredAssignments));

        // Среднее количество ролей на пользователя
        double avgRolesPerUser = userManager.count() > 0 ?
                (double) assignmentManager.count() / userManager.count() : 0;
        sb.append(String.format("Average roles per user: %.2f\n", avgRolesPerUser));

        // Топ-3 самых популярных ролей
        Map<String, Integer> roleCounts = new HashMap<>();
        for (RoleAssignment ra : assignmentManager.findAll()) {
            String roleName = ra.role().getName();
            roleCounts.put(roleName, roleCounts.getOrDefault(roleName, 0) + 1);
        }

        sb.append("Top 3 most popular roles:\n");
        roleCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> sb.append(String.format("  - %s: %d assignments\n", entry.getKey(), entry.getValue())));

        sb.append("=============================================\n");
        return sb.toString();
    }

    public void shutdown() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
        if (auditLog != null) {
            auditLog.shutdown();
        }
    }
}