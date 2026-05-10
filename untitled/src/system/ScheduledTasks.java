package system;

import managers.AssignmentManager;
import models.RoleAssignment;
import models.TemporaryAssignment;
import utils.DateUtils;
import utils.ConsoleUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduledTasks {
    private final RBACSystem system;
    private final AtomicInteger expiredCount;

    public ScheduledTasks(RBACSystem system) {
        this.system = system;
        this.expiredCount = new AtomicInteger(0);
    }

    public void startExpiredAssignmentsChecker(long intervalSeconds) {
        system.getBackgroundExecutor().scheduleAtFixedRate(() -> {
            try {
                checkAndMarkExpiredAssignments();
            } catch (Exception e) {
                System.err.println("[Scheduler] Error checking expired assignments: " + e.getMessage());
            }
        }, 5, intervalSeconds, TimeUnit.SECONDS);

        System.out.println("[Scheduler] Started expired assignments checker every " + intervalSeconds + " seconds");
    }

    private void checkAndMarkExpiredAssignments() {
        AssignmentManager assignmentManager = system.getAssignmentManager();
        List<RoleAssignment> allAssignments = assignmentManager.findAll();

        int expiredFound = 0;

        for (RoleAssignment assignment : allAssignments) {
            if (assignment instanceof TemporaryAssignment temp && temp.isActive()) {
                if (temp.isExpired()) {
                    assignmentManager.revokeAssignment(assignment.assignmentId());
                    expiredFound++;

                    system.getAuditLog().log("AUTO_REVOKE", "scheduler",
                            temp.user().username(),
                            "Temporary role " + temp.role().getName() + " expired");
                }
            }
        }

        if (expiredFound > 0) {
            expiredCount.addAndGet(expiredFound);
            System.out.println("[Scheduler] Auto-revoked " + expiredFound + " expired assignments");
        }
    }

    public void startStatisticsLogger(long intervalSeconds) {
        system.getBackgroundExecutor().scheduleAtFixedRate(() -> {
            try {
                logStatistics();
            } catch (Exception e) {
                System.err.println("[Scheduler] Error logging statistics: " + e.getMessage());
            }
        }, 10, intervalSeconds, TimeUnit.SECONDS);

        System.out.println("[Scheduler] Started statistics logger every " + intervalSeconds + " seconds");
    }

    private void logStatistics() {
        String stats = system.generateStatistics();
        system.getAuditLog().log("STATS_REPORT", "scheduler", "system",
                "Auto-generated statistics: users=" + system.getUserManager().count() +
                        ", roles=" + system.getRoleManager().count() +
                        ", assignments=" + system.getAssignmentManager().count());

        System.out.println("\n[Scheduler] Auto-generated statistics at " + DateUtils.getCurrentDateTime());
        System.out.println(stats);
    }

    public void startExpirationWarningChecker(long intervalSeconds) {
        system.getBackgroundExecutor().scheduleAtFixedRate(() -> {
            try {
                checkExpiringSoonAssignments();
            } catch (Exception e) {
                System.err.println("[Scheduler] Error checking expiring assignments: " + e.getMessage());
            }
        }, 5, intervalSeconds, TimeUnit.SECONDS);
    }

    private void checkExpiringSoonAssignments() {
        AssignmentManager assignmentManager = system.getAssignmentManager();
        List<RoleAssignment> allAssignments = assignmentManager.findAll();

        for (RoleAssignment assignment : allAssignments) {
            if (assignment instanceof TemporaryAssignment temp && temp.isActive()) {
                String warning = DateUtils.getExpirationWarning(temp.getExpiresAt());
                if (warning.contains("WARNING") || warning.equals("EXPIRES TODAY")) {
                    system.getAuditLog().log("EXPIRATION_WARNING", "scheduler",
                            temp.user().username(),
                            "Role " + temp.role().getName() + ": " + warning);
                }
            }
        }
    }

    public void startAllSchedulers() {
        startExpiredAssignmentsChecker(30);
        startStatisticsLogger(60);
        startExpirationWarningChecker(30);

        System.out.println(ConsoleUtils.formatBox("SCHEDULED TASKS STARTED"));
        System.out.println("  - Expired assignments checker: every 30 seconds");
        System.out.println("  - Statistics logger: every 60 seconds");
        System.out.println("  - Expiration warning checker: every 30 seconds");
    }

    public int getExpiredCount() {
        return expiredCount.get();
    }

    public void printStatus() {
        ConsoleUtils.printHeader("SCHEDULED TASKS STATUS");
        System.out.println("Auto-revoked expired assignments: " + expiredCount.get());
        System.out.println("Active scheduler threads: " + Thread.activeCount());
    }
}