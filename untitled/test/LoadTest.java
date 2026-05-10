import system.RBACSystem;
import models.User;
import models.Role;
import models.PermanentAssignment;
import models.AssignmentMetadata;
import filters.UserFilters;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Starting Load Test ===\n");

        RBACSystem system = new RBACSystem();
        system.initialize();
        system.setCurrentUser("load_tester");

        int threadCount = 10;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Random random = new Random();

                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        int operation = random.nextInt(5);

                        switch (operation) {
                            case 0: // Create user
                                String username = "load_user_" + threadId + "_" + j;
                                User user = User.validate(username, "Load Test User", username + "@test.com");
                                system.getUserManager().add(user);
                                successCount.incrementAndGet();
                                break;

                            case 1: // Update user
                                List<User> users = system.getUserManager().findAll();
                                if (!users.isEmpty()) {
                                    User u = users.get(random.nextInt(users.size()));
                                    system.getUserManager().update(u.username(),
                                            "Updated Name", "updated@test.com");
                                    successCount.incrementAndGet();
                                }
                                break;

                            case 2: // Create role
                                String roleName = "load_role_" + threadId + "_" + j;
                                Role role = new Role(roleName, "Load test role");
                                system.getRoleManager().add(role);
                                successCount.incrementAndGet();
                                break;

                            case 3: // Filter search
                                List<User> filtered = system.getUserManager()
                                        .findByFilterParallel(UserFilters.byUsernameContains("load"));
                                successCount.incrementAndGet();
                                break;

                            case 4: // Get statistics
                                system.generateStatistics();
                                successCount.incrementAndGet();
                                break;
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        System.out.println("=== Load Test Results ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Total operations attempted: " + (threadCount * operationsPerThread));
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + errorCount.get());
        System.out.println("Total time: " + (endTime - startTime) + " ms");
        System.out.println("\nFinal system state:");
        System.out.println("Users: " + system.getUserManager().count());
        System.out.println("Roles: " + system.getRoleManager().count());
        System.out.println("Assignments: " + system.getAssignmentManager().count());

        boolean hasDuplicates = checkForDuplicates(system);
        if (!hasDuplicates) {
            System.out.println("\nNo duplicate usernames found - PASSED");
        } else {
            System.out.println("\nDuplicate usernames found - FAILED");
        }

        System.out.println("\n=== Load Test Completed ===");
    }

    private static boolean checkForDuplicates(RBACSystem system) {
        List<User> users = system.getUserManager().findAll();
        Set<String> usernames = new HashSet<>();
        for (User u : users) {
            if (usernames.contains(u.username())) {
                System.out.println("Duplicate found: " + u.username());
                return true;
            }
            usernames.add(u.username());
        }
        return false;
    }
}