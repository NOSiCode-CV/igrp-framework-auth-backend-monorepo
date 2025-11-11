package cv.igrp.framework.auth.wso2;

import cv.igrp.framework.auth.core.exception.IAMException;
import cv.igrp.framework.auth.wso2.application.Application;
import cv.igrp.framework.auth.wso2.application.ApplicationService;
import cv.igrp.framework.auth.wso2.application.dto.CreateApplicationRequest;
import cv.igrp.framework.auth.wso2.application.dto.UpdateApplicationRequest;
import cv.igrp.framework.auth.wso2.group.GroupService;
import cv.igrp.framework.auth.wso2.shared.Resource;
import cv.igrp.framework.auth.wso2.shared.Wso2ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Wso2AdapterApplicationTest {

    @InjectMocks
    private Wso2Adapter underTest;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private GroupService groupService;

    private final ArgumentCaptor<CreateApplicationRequest> createApplicationRequestCaptor =
            ArgumentCaptor.forClass(CreateApplicationRequest.class);
    private final ArgumentCaptor<Application> applicationArgumentCaptor =
            ArgumentCaptor.forClass(Application.class);
    private final ArgumentCaptor<UpdateApplicationRequest> updateApplicationRequest =
            ArgumentCaptor.forClass(UpdateApplicationRequest.class);


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void itShouldStartContext() {
        assertNotNull(underTest);
    }

    @Test
    void itShouldThrowException_WhenApplicationServiceReturns_ApiCallFalse_WhenCreating_NewApplication() {
        //... Given
        String applicationName = "ApplicationName";
        when(applicationService.createApplication(createApplicationRequestCaptor.capture()))
                .thenReturn(Wso2ApiResponse.<Application>builder()
                        .failureMessage("failure Message")
                        .successfulCall(false)
                        .build());
        // When & Then
        assertThatThrownBy(() -> underTest.createApplication(applicationName))
                .isExactlyInstanceOf(IAMException.class)
                .hasMessage("failure Message");
    }

    @Test
    void itShouldReturnApplicationModel_WhenApplicationServiceReturn_ApiCallTrue_WhenCreating_NewApplication() throws IAMException {
        // Given
        String applicationName = "ApplicationName";
        String applicationId = UUID.randomUUID().toString();
        AtomicReference<Wso2ApiResponse<Application>> capturedResponse = new AtomicReference<>();

        when(applicationService.createApplication(createApplicationRequestCaptor.capture()))
                .thenAnswer(invocation -> {
                    CreateApplicationRequest req = invocation.getArgument(0);
                    Wso2ApiResponse<Application> response = Wso2ApiResponse.<Application>builder()
                            .successfulCall(true)
                            .resourceResponse(new Application(applicationId, req.getName()))
                            .build();
                    capturedResponse.set(response);
                    return response;
                });
        when(groupService.createGroup(anyString()))
                .thenAnswer(ans -> {
                    return Wso2ApiResponse.<Resource>builder()
                            .successfulCall(true)
                            .resourceResponse(new Resource(UUID.randomUUID().toString(), applicationName))
                            .build();
                });

        // When
        underTest.createApplication(applicationName);

        // Then
        CreateApplicationRequest capturedRequest = createApplicationRequestCaptor.getValue();
        assertThat(capturedRequest.getName()).isEqualTo(applicationName);

        Wso2ApiResponse<Application> result = capturedResponse.get();
        assertThat(result).isNotNull();
        assertThat(result.isSuccessfulCall()).isTrue();
        assertThat(result.getResourceResponse()).isNotNull();
        assertThat(result.getResourceResponse().id()).isEqualTo(applicationId);
        assertThat(result.getResourceResponse().name()).isEqualTo(applicationName);
    }

    @Test
    void itShouldThrowException_WhenApplicationServiceDidNotFindApplication_WhenDeletingApplication() {
        //... Given
        String applicationName = "ApplicationName";
        when(applicationService.getApplicationByName(applicationArgumentCaptor.capture()))
                .thenReturn(Optional.empty());
        // When & Then
        assertThatThrownBy(() -> underTest.deleteApplication(applicationName))
                .isExactlyInstanceOf(IAMException.class);
    }

    @Test
    void itShouldThrowException_WhenApplicationServiceFindApplication_AndApplicationService_ReturnsApiCall_False_WhenDeletingApplication() {
        //... Given
        String applicationName = "ApplicationName";
        String applicationId = null;
        Application application = new Application(applicationId, applicationName);
        Wso2ApiResponse<Application> expectedResponse = Wso2ApiResponse.<Application>builder()
                .failureMessage("Application not found.")
                .successfulCall(false)
                .build();

        when(applicationService.getApplicationByName(applicationArgumentCaptor.capture()))
                .thenReturn(Optional.of(application));
        when(applicationService.deleteApplication(applicationArgumentCaptor.capture()))
                .thenReturn(expectedResponse);
        // When & Then
        assertThatThrownBy(() -> underTest.deleteApplication(applicationName))
                .isExactlyInstanceOf(IAMException.class);
    }

    @Test
    void itShouldDeleteApplication_WhenApplicationServiceFindApplication_AndApplicationService_ReturnsApiCall_True_WhenDeletingApplication() {
        // Given
        String applicationName = "ApplicationName";
        String applicationId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();

        Application application = new Application(applicationId, applicationName);
        Resource group = new Resource(groupId, applicationName);

        Wso2ApiResponse<Application> deleteAppResponse = Wso2ApiResponse.<Application>builder()
                .successfulCall(true)
                .resourceResponse(application)
                .build();

        Wso2ApiResponse<Resource> deleteGroupResponse = Wso2ApiResponse.<Resource>builder()
                .successfulCall(true)
                .resourceResponse(group)
                .build();

        // Mock: application is found
        when(applicationService.getApplicationByName(applicationArgumentCaptor.capture()))
                .thenReturn(Optional.of(application));

        // Mock: application is deleted
        when(applicationService.deleteApplication(applicationArgumentCaptor.capture()))
                .thenReturn(deleteAppResponse);

        // Mock: group is found
        when(groupService.getGroupByName(eq(applicationName)))
                .thenReturn(Optional.of(group));

        // Mock: group is deleted
        when(groupService.deleteGroup(eq(group)))
                .thenReturn(deleteGroupResponse);

        // When
        assertThatCode(() -> underTest.deleteApplication(applicationName))
                .doesNotThrowAnyException();

        // Then
        verify(applicationService).getApplicationByName(applicationArgumentCaptor.capture());
        Application captured = applicationArgumentCaptor.getValue();
        assertThat(captured.name()).isEqualTo(applicationName);
    }

    @Test
    void itShouldThrowException_WhenApplicationServiceDidNotFindApplication_WhenUpdatingApplication() {
        //... Given
        String applicationOldName = "ApplicationName";
        String applicationNewName = "ApplicationName";
        when(applicationService.getApplicationByName(applicationArgumentCaptor.capture()))
                .thenReturn(Optional.empty());
        // When & Then
        assertThatThrownBy(() -> underTest.updateApplication(applicationOldName, applicationNewName))
                .isExactlyInstanceOf(IAMException.class);
    }

    @Test
    void itShouldThrowException_WhenApplicationServiceFindApplication_WhenUpdatingApplication_ReturnsApiCall_False_UpdatingApplication() {
        //... Given
        String applicationOldName = "ApplicationName";
        String applicationNewName = "ApplicationName";
        String applicationId = UUID.randomUUID().toString();
        Application application = new Application(applicationId, applicationOldName);
        Wso2ApiResponse<Application> expectedResponse = Wso2ApiResponse.<Application>builder()
                .failureMessage("Unable to update application")
                .successfulCall(false)
                .build();
        when(applicationService.getApplicationByName(applicationArgumentCaptor.capture()))
                .thenReturn(Optional.of(application));
        when(applicationService.updateApplication(applicationArgumentCaptor.capture(), updateApplicationRequest.capture()))
                .thenReturn(expectedResponse);
        // When & Then
        assertThatThrownBy(() -> underTest.updateApplication(applicationOldName, applicationNewName))
                .isExactlyInstanceOf(IAMException.class);
    }

    @Test
    void itShouldUpdate_WhenApplicationServiceFindApplication_WhenUpdatingApplication_ReturnsApiCall_True_UpdatingApplication() throws IAMException {
        // Given
        String applicationOldName = "ApplicationName";
        String applicationNewName = "NewApplicationName";
        String applicationId = UUID.randomUUID().toString();

        Application existingApp = new Application(applicationId, applicationOldName);
        Resource existingGroup = new Resource(UUID.randomUUID().toString(), applicationOldName);
        AtomicReference<Wso2ApiResponse<Application>> capturedResponse = new AtomicReference<>();

        // Mock applicationService.getApplicationByName
        when(applicationService.getApplicationByName(argThat(app -> applicationOldName.equals(app.name()))))
                .thenReturn(Optional.of(existingApp));

        // Mock applicationService.updateApplication
        when(applicationService.updateApplication(eq(existingApp), any(UpdateApplicationRequest.class)))
                .thenAnswer(invocation -> {
                    UpdateApplicationRequest req = invocation.getArgument(1);
                    Wso2ApiResponse<Application> response = Wso2ApiResponse.<Application>builder()
                            .successfulCall(true)
                            .resourceResponse(new Application(applicationId, req.getName()))
                            .build();
                    capturedResponse.set(response);
                    return response;
                });

        // Mock groupService.getGroupByName
        when(groupService.getGroupByName(applicationOldName)).thenReturn(Optional.of(existingGroup));

        // Mock groupService.updateGroupName
        when(groupService.updateGroupName(eq(existingGroup), any(Resource.class)))
                .thenReturn(Wso2ApiResponse.<Resource>builder().successfulCall(true).build());

        // When
        underTest.updateApplication(applicationOldName, applicationNewName);

        // Then
        Wso2ApiResponse<Application> result = capturedResponse.get();
        assertThat(result).isNotNull();
        assertThat(result.isSuccessfulCall()).isTrue();
        assertThat(result.getResourceResponse()).isNotNull();
        assertThat(result.getResourceResponse().name()).isEqualTo(applicationNewName);
    }
}