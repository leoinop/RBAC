package src.managers;
import models.Role;
import models.Permission;
import src.filters.RoleFilter;
import src.repositories.Repository;

import java.util.*;
import java.util.stream.Collectors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById;
    private final Map<String, Role> rolesByName;
    private final ReentrantReadWriteLock lock;
    private final AssignmentManager assignmentManager;

    public RoleManager(AssignmentManager assignmentManager) {
        this.rolesById = new ConcurrentHashMap<>();
        this.rolesByName = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String roleId = role.getId();
        String roleName = role.getName();

        lock.writeLock().lock();
        try {
            if (rolesById.containsKey(roleId)) {
                throw new IllegalArgumentException("Role with id '" + roleId + "' already exists");
            }
            if (rolesByName.containsKey(roleName)) {
                throw new IllegalArgumentException("Role with name '" + roleName + "' already exists");
            }
            rolesById.put(roleId, role);
            rolesByName.put(roleName, role);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            if (assignmentManager != null && assignmentManager.findByRole(role).size() > 0) {
                throw new IllegalStateException("Cannot delete role '" + role.getName() + "' because it is assigned to users");
            }

            Role removed = rolesById.remove(role.getId());
            if (removed != null) {
                rolesByName.remove(role.getName());
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Role> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(rolesById.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return rolesById.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            rolesById.clear();
            rolesByName.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Role> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesByName.get(name));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilterParallel(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return rolesById.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        List<Role> result = findByFilter(filter);
        if (sorter != null) {
            result.sort(sorter);
        }
        return result;
    }

    public boolean exists(String name) {
        if (name == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return rolesByName.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (roleName == null) {
            throw new IllegalArgumentException("Role name cannot be null");
        }
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        lock.writeLock().lock();
        try {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role with name '" + roleName + "' not found");
            }
            role.addPermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null) {
            throw new IllegalArgumentException("Role name cannot be null");
        }
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        lock.writeLock().lock();
        try {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role with name '" + roleName + "' not found");
            }
            role.removePermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return Collections.emptyList();
        }

        lock.readLock().lock();
        try {
            return rolesById.values().stream()
                    .filter(role -> role.hasPermission(permissionName, resource))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}