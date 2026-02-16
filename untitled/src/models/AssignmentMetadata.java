package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AssignmentMetadata {
        if (assignedBy == null || assignedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("AssignedBy cannot be null or empty");
        }
        if (assignedAt == null || assignedAt.trim().isEmpty()) {
            throw new IllegalArgumentException("AssignedAt cannot be null or empty");
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String now = LocalDateTime.now().format(FORMATTER);
        return new AssignmentMetadata(assignedBy, now, reason);
    }

    public String format() {
        if (reason != null && !reason.isEmpty()) {
            return String.format("Assigned by: %s at %s, Reason: %s", assignedBy, assignedAt, reason);
        } else {
            return String.format("Assigned by: %s at %s", assignedBy, assignedAt);
        }
    }
}