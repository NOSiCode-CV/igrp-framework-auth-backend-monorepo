package cv.igrp.framework.auth.wso2;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.wso2.application.Application;
import cv.igrp.framework.auth.wso2.application.ApplicationService;
import cv.igrp.framework.auth.wso2.group.GroupService;
import cv.igrp.framework.auth.wso2.role.Role;
import cv.igrp.framework.auth.wso2.role.RoleService;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import cv.igrp.framework.auth.wso2.user.UserService;
import cv.igrp.framework.auth.wso2.user.dto.Wso2UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class Wso2AdapterRoleTest {

    @InjectMocks
    private Wso2Adapter underTest;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private GroupService groupService;
    @Mock
    private RoleService roleService;
    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void itShouldStartContext() {
        assertNotNull(underTest);
    }

    @Test
    void itShouldCreateRole_WhenApplicationAndDepartmentExist() throws IAMException {
        // Given
        String applicationName = "myApp";
        String departmentName = "app_myapp_finance";
        String roleName = "admin";
        Application application = new Application(UUID.randomUUID().toString(), "app_myapp");
        Resource department = new Resource(UUID.randomUUID().toString(), departmentName);
        Role role = new Role(null, roleName);

        when(applicationService.getApplicationByName(any(Application.class)))
                .thenReturn(Optional.of(application));

        when(groupService.getGroupByName(departmentName))
                .thenReturn(Optional.of(department));

        when(roleService.createRole(any(Role.class)))
                .thenReturn(Wso2ApiResponse.<Resource>builder()
                        .successfulCall(true)
                        .resourceResponse(new Resource(UUID.randomUUID().toString(), role.name()))
                        .build());

        // When & Then
        assertThatCode(() -> underTest.createRole(applicationName, departmentName, roleName))
                .doesNotThrowAnyException();
    }

    @Test
    void itShouldThrowException_WhenRoleDoesNotExist_OnDelete() {
        // Given
        String roleName = "non_existing_role";
        when(roleService.findByName(roleName)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> underTest.deleteRole(roleName))
                .isInstanceOf(IAMException.class);
    }

    @Test
    void itShouldDeleteRole_WhenRoleExists_AndDeletionSucceeds() throws IAMException {
        // Given
        String roleName = "app_myapp_admin";
        Role role = new Role(UUID.randomUUID().toString(), roleName);

        when(roleService.findByName(roleName)).thenReturn(Optional.of(role));

        when(roleService.deleteRole(role)).thenReturn(
                Wso2ApiResponse.<Resource>builder().successfulCall(true).resourceResponse(new Resource(role.id(), role.name())).build()
        );

        // When & Then
        assertThatCode(() -> underTest.deleteRole(roleName)).doesNotThrowAnyException();
    }

    @Test
    void itShouldAssignRoleToUser_WhenRoleAndUserExist() throws IAMException {
        // Given
        String roleName = "app_myapp_admin";
        String userId = "user-123";
        Role role = new Role(UUID.randomUUID().toString(), roleName);
        Wso2UserResponse user = new Wso2UserResponse();
        user.setId(userId);
        user.setUserName("john.doe");

        when(roleService.findByName(roleName)).thenReturn(Optional.of(role));
        when(userService.findUserById(userId)).thenReturn(
                Wso2ApiResponse.<Wso2UserResponse>builder().successfulCall(true).resourceResponse(user).build()
        );
        when(roleService.assignRoleToUser(role, user)).thenReturn(
                Wso2ApiResponse.<Resource>builder().successfulCall(true).build()
        );

        // When & Then
        assertThatCode(() -> underTest.assignRoleToUser(roleName, userId)).doesNotThrowAnyException();
    }
}