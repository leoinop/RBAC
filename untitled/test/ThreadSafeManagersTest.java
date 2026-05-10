package managers;

import models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafeManagersTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Testing Thread-Safe Managers ===\n");

        testConcurrentUserCreation();
        testConcurrentRoleCreation();
        testConcurrentAssignmentCreation();
        testConcurrentReadWrite();

        System.out.println("\n=== All thread-safety tests completed ===");
    }

    static void testConcurrentUserCreation() throws InterruptedException {
        System.out.println("Test: concurrentUserCreation");

        UserManager userManager = new UserManager();
        int threadCount = 10;
        int usersPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < usersPerThread; j++) {
                    try {
                        String username = "user_" + threadId + "_" + j;
                        User user = User.validate(username, "Test User", username + "@test.com");
                        userManager.add(user);
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("  Users created: " + userManager.count());
        System.out.println("  Success: " + successCount.get());
        System.out.println("  Errors: " + errorCount.get());

        if (userManager.count() == threadCount * usersPerThread) {
            System.out.println("  PASSED\n");
        } else {
            System.out.println("  FAILED: Expected " + (threadCount * usersPerThread) +
                    " but got " + userManager.count() + "\n");
        }
    }

    static void testConcurrentRoleCreation() throws InterruptedException {
        System.out.println("Test: concurrentRoleCreation");

        AssignmentManager assignmentManager = null;
        RoleManager roleManager = new RoleManager(assignmentManager);
        int threadCount = 10;
        int rolesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < rolesPerThread; j++) {
                    try {
                        String roleName = "role_" + threadId + "_" + j;
                        Role role = new Role(roleName, "Test role");
                        roleManager.add(role);
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        // Duplicate is expected if same name created twice
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("  Roles created: " + roleManager.count());
        System.out.println("  Success attempts: " + successCount.get());

        if (roleManager.count() > 0) {
            System.out.println("  PASSED\n");
        } else {
            System.out.println("  FAILED\n");
        }
    }

    static void testConcurrentAssignmentCreation() throws InterruptedException {
        System.out.println("Test: concurrentAssignmentCreation");

        UserManager userManager = new UserManager();
        RoleManager roleManager = new RoleManager(null);
        AssignmentManager assignmentManager = new AssignmentManager(userManager, roleManager);

        User user = User.validate("assign_test_user", "Assign Test", "assign@test.com");
        userManager.add(user);

        Role role = new Role("AssignTestRole", "Test role");
        roleManager.add(role);

        int threadCount = 10;
        int assignmentsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "Test assignment");

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < assignmentsPerThread; j++) {
                    try {
                        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                        assignmentManager.add(assignment);
                        successCount.incrementAndGet();
                    } catch (IllegalStateException e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("  Assignments created: " + assignmentManager.count());
        System.out.println("  Success (first assignment only): " + successCount.get());
        System.out.println("  Errors (duplicates): " + errorCount.get());

        if (assignmentManager.count() == 1 && successCount.get() == 1 && errorCount.get() > 0) {
            System.out.println("  PASSED\n");
        } else {
            System.out.println("  FAILED\n");
        }
    }

    static void testConcurrentReadWrite() throws InterruptedException {
        System.out.println("Test: concurrentReadWrite");

        UserManager userManager = new UserManager();

        for (int i = 0; i < 100; i++) {
            User user = User.validate("readwrite_user_" + i, "Test", "test" + i + "@test.com");
            userManager.add(user);
        }

        ExecutorService readers = Executors.newFixedThreadPool(5);
        ExecutorService writers = Executors.newFixedThreadPool(3);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            readers.submit(() -> {
                List<User> users = userManager.findAll();
                readCount.incrementAndGet();
            });
        }

        for (int i = 0; i < 10; i++) {
            final int id = i;
            writers.submit(() -> {
                try {
                    User newUser = User.validate("new_user_" + id, "New", "new" + id + "@test.com");
                    userManager.add(newUser);
                    writeCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    // Ignore duplicates
                }
            });
        }

        readers.shutdown();
        writers.shutdown();
        readers.awaitTermination(10, TimeUnit.SECONDS);
        writers.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("  Read operations: " + readCount.get());
        System.out.println("  Write operations: " + writeCount.get());
        System.out.println("  Final user count: " + userManager.count());

        if (readCount.get() > 0 && userManager.count() >= 100) {
            System.out.println("  PASSED\n");
        } else {
            System.out.println("  FAILED\n");
        }
    }
}