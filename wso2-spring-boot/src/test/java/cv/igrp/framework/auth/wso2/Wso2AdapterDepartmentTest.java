package cv.igrp.framework.auth.wso2;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.wso2.application.Application;
import cv.igrp.framework.auth.wso2.application.ApplicationService;
import cv.igrp.framework.auth.wso2.group.GroupService;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class Wso2AdapterDepartmentTest {

    @InjectMocks
    private Wso2Adapter underTest;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private GroupService groupService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void itShouldStartContext() {
        assertNotNull(underTest);
    }

    @Test
    void itShouldCreateDepartment_WhenApplicationExists_AndGroupIsCreatedSuccessfully() throws IAMException {
        // Given
        String applicationName = "myApp";
        String departmentName = "Finance";
        Application mockApp = new Application(UUID.randomUUID().toString(), "app_myapp");

        when(applicationService.getApplicationByName(any(Application.class)))
                .thenReturn(Optional.of(mockApp));

        when(groupService.createGroup(eq(departmentName)))
                .thenReturn(Wso2ApiResponse.<Resource>builder()
                        .successfulCall(true)
                        .resourceResponse(new Resource(UUID.randomUUID().toString(), departmentName))
                        .build());

        // When & Then
        assertThatCode(() -> underTest.createDepartment(applicationName, departmentName))
                .doesNotThrowAnyException();
    }

    @Test
    void itShouldThrowException_WhenDepartmentToDeleteDoesNotExist() {
        // Given
        String departmentName = "Finance";
        when(groupService.getGroupByName(departmentName)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> underTest.deleteDepartment(departmentName))
                .isInstanceOf(IAMException.class);
    }

    @Test
    void itShouldDeleteDepartment_WhenGroupExistsAndDeletionSucceeds() throws IAMException {
        // Given
        String departmentName = "Finance";
        Resource group = new Resource(UUID.randomUUID().toString(), departmentName);

        when(groupService.getGroupByName(departmentName)).thenReturn(Optional.of(group));
        when(groupService.deleteGroup(group)).thenReturn(Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .resourceResponse(group)
                .build());

        // When & Then
        assertThatCode(() -> underTest.deleteDepartment(departmentName)).doesNotThrowAnyException();
    }

    @Test
    void itShouldUpdateDepartment_WhenGroupExists_AndUpdateSucceeds() throws IAMException {
        // Given
        String departmentName = "app_myApp_finance";
        String newDepartmentName = "marketing";
        Resource department = new Resource(UUID.randomUUID().toString(), departmentName);

        when(groupService.getGroupByName(departmentName)).thenReturn(Optional.of(department));

        when(groupService.updateGroupName(eq(department), any(Resource.class)))
                .thenReturn(Wso2ApiResponse.<Resource>builder().successfulCall(true).build());

        // When & Then
        assertThatCode(() -> underTest.updateDepartment(departmentName, newDepartmentName))
                .doesNotThrowAnyException();
    }
}