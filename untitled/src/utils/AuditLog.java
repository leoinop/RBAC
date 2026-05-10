package utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {
        public String format() {
            return String.format("[%s] %s | Performer: %s | Target: %s | Details: %s",
                    timestamp, action, performer, target, details);
        }

        public String toCsv() {
            return String.format("%s,%s,%s,%s,%s",
                    timestamp, action, performer, target,
                    details != null ? details.replace(",", ";") : "");
        }
    }

    private final List<AuditEntry> entries;
    private final BlockingQueue<AuditEntry> queue;
    private final ExecutorService consumerExecutor;
    private volatile boolean running;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLog() {
        this.entries = new CopyOnWriteArrayList<>();
        this.queue = new LinkedBlockingQueue<>();
        this.consumerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AuditLogConsumer");
            t.setDaemon(true);
            return t;
        });
        this.running = true;
        startConsumer();
    }

    private void startConsumer() {
        consumerExecutor.submit(() -> {
            while (running) {
                try {
                    AuditEntry entry = queue.poll(1, TimeUnit.SECONDS);
                    if (entry != null) {
                        entries.add(entry);
                        System.out.println("[AUDIT] " + entry.format());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
        queue.offer(entry);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        if (performer == null) {
            return Collections.emptyList();
        }
        return entries.stream()
                .filter(entry -> entry.performer().equalsIgnoreCase(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        if (action == null) {
            return Collections.emptyList();
        }
        return entries.stream()
                .filter(entry -> entry.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByTarget(String target) {
        if (target == null) {
            return Collections.emptyList();
        }
        return entries.stream()
                .filter(entry -> entry.target().equalsIgnoreCase(target))
                .collect(Collectors.toList());
    }

    public void printLog() {
        System.out.println("\n========== AUDIT LOG ==========");
        if (entries.isEmpty()) {
            System.out.println("No audit entries");
        } else {
            for (AuditEntry entry : entries) {
                System.out.println(entry.format());
            }
        }
        System.out.println("================================\n");
    }

    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Timestamp,Action,Performer,Target,Details");
            writer.newLine();
            for (AuditEntry entry : entries) {
                writer.write(entry.toCsv());
                writer.newLine();
            }
        }
    }

    public void loadFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    AuditEntry entry = new AuditEntry(parts[0], parts[1], parts[2], parts[3], parts[4]);
                    entries.add(entry);
                }
            }
        }
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public void shutdown() {
        running = false;
        consumerExecutor.shutdown();
    }
}