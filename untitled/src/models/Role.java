package models;

import java.util.*;

public class Role {
    private final String id;
    private String name;
    private String description;
    private final Set<models.Permission> permissions;

    public Role(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    public Role(String id, String name, String description, Set<models.Permission> permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>(permissions);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addPermission(models.Permission permission) {
        permissions.add(permission);
    }

    public void removePermission(models.Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(models.Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        for (models.Permission p : permissions) {
            if (p.name().equalsIgnoreCase(permissionName) &&
                    p.resource().equalsIgnoreCase(resource)) {
                return true;
            }
        }
        return false;
    }

    public Set<models.Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description));
        sb.append(String.format("Permissions (%d):\n", permissions.size()));

        for (models.Permission p : permissions) {
            sb.append(" - ").append(p.format()).append("\n");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Role{id='%s', name='%s', description='%s', permissions=%d}",
                id, name, description, permissions.size());
    }
}