package managers;

import models.Role;
import models.Permission;
import filters.RoleFilter;
import repository.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    // Метод репозитория
    @Override
    public void add(Role role) {
        if (role == null)
            throw new IllegalArgumentException("Role не может быть пустой");

        if (rolesById.containsKey(role.getId()))
            throw new IllegalArgumentException("Role ID уже есть");

        if (rolesByName.containsKey(role.getName()))
            throw new IllegalArgumentException("Role name уже есть");

        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) return false;

        rolesByName.remove(role.getName());
        return rolesById.remove(role.getId()) != null;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    // Доп методы
    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        return rolesById.values().stream()
                .filter(filter::test)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null)
            throw new NoSuchElementException("Role не найдена");

        role.getPermissions().add(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null)
            throw new NoSuchElementException("Role не найдена");

        role.getPermissions().remove(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(r -> r.getPermissions().stream()
                        .anyMatch(p -> p.getName().equals(permissionName)
                                && p.getResource().equals(resource)))
                .collect(Collectors.toList());
    }
}