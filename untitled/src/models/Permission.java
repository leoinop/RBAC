package models;

public record Permission(String name, String resource, String description) {

    public Permission {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name cannot be null or empty");
        }
        String normalizedName = name.trim().toUpperCase();
        if (normalizedName.contains(" ")) {
            throw new IllegalArgumentException("Permission name cannot contain spaces");
        }

        if (resource == null || resource.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource cannot be null or empty");
        }
        String normalizedResource = resource.trim().toLowerCase();

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        String normalizedDescription = description.trim();
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = true;
        boolean resourceMatches = true;

        if (namePattern != null && !namePattern.isEmpty()) {
            nameMatches = this.name.contains(namePattern.toUpperCase());
        }

        if (resourcePattern != null && !resourcePattern.isEmpty()) {
            resourceMatches = this.resource.contains(resourcePattern.toLowerCase());
        }

        return nameMatches && resourceMatches;
    }
}