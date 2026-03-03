package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.constants.AppType;
import cv.igrp.platform.access.client.model.ApplicationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationsApiTest {

    @Mock
    private IApiClient apiClient;

    private ApplicationsApi applicationsApi;

    private final ApplicationDTO sampleApp = new ApplicationDTO();

    @BeforeEach
    void setUp() {

        applicationsApi = new ApplicationsApi(apiClient);

        // Setup sample application
        sampleApp.setId(123);
        sampleApp.setName("Test Application");
        sampleApp.setCode("APP001");
        sampleApp.setType(AppType.INTERNAL);
    }

    @Test
    void getApplicationById_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(ApplicationDTO.class)))
                .thenReturn(sampleApp);

        ApplicationDTO result = applicationsApi.getApplicationById(123);

        assertNotNull(result);
        assertEquals("Test Application", result.getName());
        verify(apiClient).invokeAPI("/api/applications/123", "GET", null, null, null, ApplicationDTO.class);
    }

    @Test
    void getApplicationById_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(ApplicationDTO.class)))
                .thenThrow(new ApiException(404, "Not Found"));

        assertThrows(ApiException.class, () -> applicationsApi.getApplicationById(123));
    }

    @Test
    void getApplicationByCode_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(ApplicationDTO.class)))
                .thenReturn(sampleApp);

        ApplicationDTO result = applicationsApi.getApplicationByCode("APP001");

        assertNotNull(result);
        assertEquals("Test Application", result.getName());
        verify(apiClient).invokeAPI("/api/applications/by-code/APP001", "GET", null, null, null, ApplicationDTO.class);
    }

    @Test
    void getApplicationByCode_ThrowsApiException() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(ApplicationDTO.class)))
                .thenThrow(new ApiException(404, "Not Found"));

        assertThrows(ApiException.class, () -> applicationsApi.getApplicationByCode("APP001"));
    }

    @Test
    void createApplication_Success() throws ApiException {
        when(apiClient.invokeAPI(eq("/api/applications"), eq("POST"), isNull(), eq(sampleApp), isNull(), eq(ApplicationDTO.class)))
                .thenReturn(sampleApp);

        ApplicationDTO result = applicationsApi.createApplication(sampleApp);

        assertNotNull(result);
        assertEquals(123, result.getId());
        verify(apiClient).invokeAPI("/api/applications", "POST", null, sampleApp, null, ApplicationDTO.class);
    }

    @Test
    void updateApplication_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("PUT"), isNull(), eq(sampleApp), isNull(), eq(ApplicationDTO.class)))
                .thenReturn(sampleApp);

        ApplicationDTO result = applicationsApi.updateApplication("APP001", sampleApp);

        assertNotNull(result);
        assertEquals("APP001", result.getCode());
        verify(apiClient).invokeAPI("/api/applications/APP001", "PUT", null, sampleApp, null, ApplicationDTO.class);
    }

    @Test
    void deleteApplication_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("DELETE"), isNull(), isNull(), isNull(), eq(String.class)))
                .thenReturn("Deleted");

        String result = applicationsApi.deleteApplication("APP001");
        assertEquals("Deleted", result);
    }

    @Test
    void getApplications_Filtered() throws ApiException, IOException, InterruptedException {
        // Setup expected applications
        ApplicationDTO app1 = new ApplicationDTO();
        app1.setId(1);
        ApplicationDTO app2 = new ApplicationDTO();
        app2.setId(2);
        List<ApplicationDTO> expectedApps = Arrays.asList(app1, app2);

        // Mock raw response handling
        when(apiClient.invokeAPIRaw(eq("/api/applications"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn("[{\"id\":1},{\"id\":2}]");

        // Execute with filters
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("name", "test");
        expectedParams.put("code", "APP");

        List<ApplicationDTO> result = applicationsApi.getApplications("APP", "test", null, null, null);

        assertEquals(2, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/applications",
                "GET",
                expectedParams,
                null,
                null
        );
    }

    @Test
    void getApplicationsByIds_Success() throws ApiException, IOException, InterruptedException {
        List<Integer> ids = Arrays.asList(1, 2, 3);

        // Mock raw response handling
        when(apiClient.invokeAPIRaw(eq("/api/applications/by-ids"), eq("POST"), isNull(), eq(ids), isNull()))
                .thenReturn("[{\"id\":1},{\"id\":2},{\"id\":3}]");

        List<ApplicationDTO> result = applicationsApi.getApplicationsByIds(ids);

        assertEquals(3, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/applications/by-ids",
                "POST",
                null,
                ids,
                null
        );
    }

    @Test
    void addApplicationCustomFields_Success() throws ApiException {
        Map<String, Object> customFields = Map.of(
                "field1", "value1",
                "field2", true
        );

        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), eq(customFields), isNull(), eq(String.class)))
                .thenReturn("Fields added");

        String result = applicationsApi.addApplicationCustomFields("APP001", customFields);
        assertEquals("Fields added", result);
        verify(apiClient).invokeAPI(
                "/api/applications/APP001/custom-fields",
                "POST",
                null,
                customFields,
                null,
                String.class
        );
    }

    @Test
    void removeApplicationCustomFields_Success() throws ApiException {
        List<String> keys = Arrays.asList("field1", "field2");

        when(apiClient.invokeAPI(anyString(), eq("DELETE"), isNull(), eq(keys), isNull(), eq(String.class)))
                .thenReturn("Fields removed");

        String result = applicationsApi.removeApplicationCustomFields("APP001", keys);
        assertEquals("Fields removed", result);
    }

}