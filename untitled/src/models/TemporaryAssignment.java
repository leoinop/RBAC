package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends models.AbstractRoleAssignment {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String expiresAt;
    private boolean autoRenew;

    public TemporaryAssignment(models.User user, models.Role role, models.AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            return now.isAfter(expiry);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isExpired(LocalDateTime checkTime) {
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            return checkTime.isAfter(expiry);
        } catch (Exception e) {
            return true;
        }
    }

    public void extend(String newExpirationDate) {
        this.expiresAt = newExpirationDate;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public String getTimeRemaining() {
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(expiry)) {
                return "Expired";
            }

            long days = ChronoUnit.DAYS.between(now, expiry);
            long hours = ChronoUnit.HOURS.between(now, expiry) % 24;
            long minutes = ChronoUnit.MINUTES.between(now, expiry) % 60;

            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        String status = isActive() ? "ACTIVE" : "EXPIRED";

        return String.format("%s\nExpires: %s\nAuto-renew: %s\nTime remaining: %s\nStatus: %s",
                baseSummary, expiresAt, autoRenew ? "Yes" : "No", getTimeRemaining(), status);
    }
}