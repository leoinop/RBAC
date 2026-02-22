package managers;

import models.*;
import filters.AssignmentFilter;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new HashMap<>();

    // ===== Repository =====

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null)
            throw new IllegalArgumentException("Assignment не может быть пустым");

        if (assignments.containsKey(assignment.getAssignmentId()))
            throw new IllegalArgumentException("Assignment уже существует");

        assignments.put(assignment.getAssignmentId(), assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        return assignments.remove(assignment.getAssignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    // ===== Additional methods =====

    public List<RoleAssignment> findByUser(User user) {
        return assignments.values().stream()
                .filter(a -> a.getUser().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        return assignments.values().stream()
                .filter(a -> a.getRole().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return assignments.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter,
                                        Comparator<RoleAssignment> sorter) {
        return assignments.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        return assignments.values().stream()
                .anyMatch(a -> a.getUser().equals(user)
                        && a.getRole().equals(role)
                        && a.isActive());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        return getUserPermissions(user).stream()
                .anyMatch(p -> p.getName().equals(permissionName)
                        && p.getResource().equals(resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        return assignments.values().stream()
                .filter(a -> a.getUser().equals(user) && a.isActive())
                .flatMap(a -> a.getRole().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null)
            throw new NoSuchElementException("Assignment не найдено");

        assignment.setActive(false);
    }
}