package models;

public interface RoleAssignment {
    String assignmentId();
    models.User user();
    models.Role role();
    models.AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();
}