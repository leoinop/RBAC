package src.managers;
import models.User;
import models.Role;
import models.Permission;
import models.RoleAssignment;
import models.PermanentAssignment;
import models.TemporaryAssignment;
import src.filters.AssignmentFilter;
import src.repositories.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignmentsById;
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final ReentrantReadWriteLock lock;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.assignmentsById = new ConcurrentHashMap<>();
        this.userManager = userManager;
        this.roleManager = roleManager;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        lock.writeLock().lock();
        try {
            Optional<User> userOpt = userManager.findByUsername(assignment.user().username());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User '" + assignment.user().username() + "' does not exist");
            }

            Optional<Role> roleOpt = roleManager.findById(assignment.role().getId());
            if (roleOpt.isEmpty()) {
                throw new IllegalArgumentException("Role with id '" + assignment.role().getId() + "' does not exist");
            }

            boolean hasActiveAssignment = assignmentsById.values().stream()
                    .filter(a -> a.user().username().equals(assignment.user().username()))
                    .filter(a -> a.role().getId().equals(assignment.role().getId()))
                    .anyMatch(RoleAssignment::isActive);

            if (hasActiveAssignment) {
                throw new IllegalStateException("User already has active assignment for this role");
            }

            assignmentsById.put(assignment.assignmentId(), assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            return assignmentsById.remove(assignment.assignmentId()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(assignmentsById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(assignmentsById.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return assignmentsById.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            assignmentsById.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }

        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> a.user().username().equals(user.username()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }

        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> a.role().getId().equals(role.getId()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        List<RoleAssignment> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public List<RoleAssignment> getActiveAssignments() {
        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> !a.isActive())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> a.user().username().equals(user.username()))
                    .filter(a -> a.role().getId().equals(role.getId()))
                    .anyMatch(RoleAssignment::isActive);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> a.user().username().equals(user.username()))
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::role)
                    .anyMatch(role -> role.hasPermission(permissionName, resource));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> a.user().username().equals(user.username()))
                    .filter(RoleAssignment::isActive)
                    .map(RoleAssignment::role)
                    .flatMap(role -> role.getPermissions().stream())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            Optional<RoleAssignment> opt = findById(assignmentId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Assignment with id '" + assignmentId + "' not found");
            }

            RoleAssignment assignment = opt.get();
            if (assignment instanceof PermanentAssignment permanent) {
                permanent.revoke();
            } else if (assignment instanceof TemporaryAssignment temp) {
                LocalDateTime past = LocalDateTime.now().minusDays(1);
                String expiredDate = past.format(FORMATTER);
                temp.extend(expiredDate);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        lock.writeLock().lock();
        try {
            Optional<RoleAssignment> opt = findById(assignmentId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Assignment with id '" + assignmentId + "' not found");
            }

            RoleAssignment assignment = opt.get();
            if (!(assignment instanceof TemporaryAssignment temp)) {
                throw new IllegalArgumentException("Assignment is not temporary");
            }

            temp.extend(newExpirationDate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findExpiredTemporaryAssignments() {
        lock.readLock().lock();
        try {
            return assignmentsById.values().stream()
                    .filter(a -> a instanceof TemporaryAssignment)
                    .filter(a -> !a.isActive())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}