package tests;

import managers.RoleManager;
import models.Permission;
import models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleManagerTest {

    private RoleManager roleManager;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager();
    }

    @Test
    void addRole_success() {
        Role role = new Role("1", "ADMIN");

        roleManager.add(role);

        assertEquals(1, roleManager.count());
        assertTrue(roleManager.exists("ADMIN"));
    }

    @Test
    void addPermission_success() {
        Role role = new Role("1", "ADMIN");
        roleManager.add(role);

        Permission permission = new Permission("READ", "USER");

        roleManager.addPermissionToRole("ADMIN", permission);

        assertEquals(1, role.getPermissions().size());
    }

    @Test
    void removePermission_success() {
        Role role = new Role("1", "ADMIN");
        roleManager.add(role);

        Permission permission = new Permission("READ", "USER");

        roleManager.addPermissionToRole("ADMIN", permission);
        roleManager.removePermissionFromRole("ADMIN", permission);

        assertEquals(0, role.getPermissions().size());
    }
}