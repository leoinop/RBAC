package models;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements models.RoleAssignment {
    private final String assignmentId;
    private final models.User user;
    private final models.Role role;
    private final models.AssignmentMetadata metadata;

    public AbstractRoleAssignment(models.User user, models.Role role, models.AssignmentMetadata metadata) {
        this.assignmentId = UUID.randomUUID().toString();
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public models.User user() {
        return user;
    }

    @Override
    public models.Role role() {
        return role;
    }

    @Override
    public models.AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    public String summary() {
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        String reason = metadata.reason() != null && !metadata.reason().isEmpty()
                ? metadata.reason() : "No reason provided";

        return String.format("[%s] %s assigned to %s by %s at %s\nReason: %s\nStatus: %s",
                assignmentType(),
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                metadata.assignedAt(),
                reason,
                status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', user='%s', role='%s'}",
                getClass().getSimpleName(), assignmentId, user.username(), role.getName());
    }
}