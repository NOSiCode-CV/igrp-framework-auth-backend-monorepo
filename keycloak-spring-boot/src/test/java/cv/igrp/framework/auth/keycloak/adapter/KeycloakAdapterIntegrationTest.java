package cv.igrp.framework.auth.keycloak.adapter;


import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.core.model.UserIdentity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class KeycloakAdapterIntegrationTest {

    //@Autowired
   //private KeycloakAdapter keycloakAdapter;

    @Test
    void testCreateApplicationIntegration() throws IAMException {
        //keycloakAdapter.createApplication("app_eSAJ");
    }

    @Test
    void testCreateParentDepartmentIntegration() throws IAMException {
       //keycloakAdapter.createDepartment("app_eSAJ", "eSAJ|department_A", null);
        //keycloakAdapter.createDepartment("app_eSAJ", "eSAJ|department_B", null);
    }

    @Test
    void testCreateSubDepartmentIntegration() throws IAMException {
        //keycloakAdapter.createDepartment("app_eSAJ", "eSAJ|department_1", "eSAJ|department_A");
    }

    @Test
    void testCreateRoleIntegration() throws IAMException {
        //keycloakAdapter.createRole("app_eSAJ", "eSAJ|department_A", "ROLE_1");
        //keycloakAdapter.createRole("app_eSAJ", "eSAJ|department_A", "ROLE_2");
        //keycloakAdapter.createRole("app_eSAJ", "eSAJ|department_B", "ROLE_3");
        //keycloakAdapter.createRole("app_eSAJ", "eSAJ|department_A", "ROLE_4");
    }

    @Test
    void testResolveUserIntegration() {
        //assertTrue(keycloakAdapter.resolveUser("demo").isPresent());
    }

    @Test
    void testAssignRoleToUserIntegration() throws IAMException {
        //keycloakAdapter.assignRoleToUser("ROLE_1", "demo");
        //keycloakAdapter.assignRoleToUser("ROLE_2", "demo");
        //keycloakAdapter.assignRoleToUser("ROLE_4", "demo");
    }

    @Test
    void testUnassignRoleFromUserIntegration() throws IAMException {
        //keycloakAdapter.unassignRoleFromUser("ROLE_4", "demo");
    }

    @Test
    void testDeleteRoleIntegration() throws IAMException {
        //keycloakAdapter.deleteRole("ROLE_4");
    }

    @Test
    void testCreateResourceIntegration() throws IAMException {
        //keycloakAdapter.createResource("eSAJ|resource_1","Resource 1 from app eSAJ", List.of("/esaj/invoice"),null);
    }

    @Test
    void testDeleteResourceIntegration() throws IAMException {
        //keycloakAdapter.deleteResource("eSAJ|resource_1");
    }

    @Test
    void testCreatePermissionIntegration() throws IAMException {
        //keycloakAdapter.createPermission("eSAJ_edit_invoice_14", "Allow to edit an invoice");
    }

    @Test
    void testDeletePermissionIntegration() throws IAMException {
        //keycloakAdapter.deletePermission("eSAJ_edit_invoice_14");
    }

    @Test
    void testAssignPermissionToRolesIntegration() throws IAMException {
        //keycloakAdapter.assignPermissionToRoles("eSAJ_edit_invoice_14", Set.of("ROLE_1", "ROLE_2"));
    }

    @Test
    void testUnassignPermissionFromRolesIntegration() throws IAMException {
       // keycloakAdapter.unassignPermissionFromRoles("eSAJ_edit_invoice_14", Set.of("ROLE_2"));
    }

    @Test
    void testAssignPermissionsToRoleIntegration() throws IAMException {
        //keycloakAdapter.assignPermissionsToRole(Set.of("eSAJ_edit_invoice_14"), "ROLE_2");
    }

    @Test
    void testUnassignPermissionsFromRoleIntegration() throws IAMException {
        //keycloakAdapter.unassignPermissionsFromRole(Set.of("eSAJ_edit_invoice_14"), "ROLE_2");
    }

}
