package system;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BackgroundExecutor {
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicLong taskCounter;

    public BackgroundExecutor() {
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("BackgroundWorker-" + System.currentTimeMillis());
            return t;
        });
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.taskCounter = new AtomicLong(0);
    }

    public Future<?> submit(Runnable task) {
        long taskId = taskCounter.incrementAndGet();
        System.out.println("[Background] Submitting task " + taskId);
        return executor.submit(() -> {
            try {
                System.out.println("[Background] Starting task " + taskId);
                task.run();
                System.out.println("[Background] Completed task " + taskId);
            } catch (Exception e) {
                System.err.println("[Background] Task " + taskId + " failed: " + e.getMessage());
            }
        });
    }

    public <T> Future<T> submit(Callable<T> task) {
        long taskId = taskCounter.incrementAndGet();
        System.out.println("[Background] Submitting callable task " + taskId);
        return executor.submit(() -> {
            try {
                System.out.println("[Background] Starting callable task " + taskId);
                T result = task.call();
                System.out.println("[Background] Completed callable task " + taskId);
                return result;
            } catch (Exception e) {
                System.err.println("[Background] Task " + taskId + " failed: " + e.getMessage());
                throw e;
            }
        });
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        System.out.println("[Background] Scheduling task with delay " + delay + " " + unit);
        return scheduledExecutor.schedule(task, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        System.out.println("[Background] Scheduling periodic task every " + period + " " + unit);
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void shutdown() {
        System.out.println("[Background] Shutting down executors...");
        executor.shutdown();
        scheduledExecutor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[Background] Executors shutdown complete");
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }
}