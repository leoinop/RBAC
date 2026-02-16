package models;

public class PermanentAssignment extends models.AbstractRoleAssignment {
    private boolean revoked = false;

    public PermanentAssignment(models.User user, models.Role role, models.AssignmentMetadata metadata) {
        super(user, role, metadata);
    }

    @Override
    public boolean isActive() {
        return !revoked;
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }
}