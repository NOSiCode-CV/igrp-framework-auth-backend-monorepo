package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.DepartmentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentsApiTest {

    @Mock
    private IApiClient apiClient;

    private DepartmentsApi departmentsApi;
    private final DepartmentDTO sampleDept = new DepartmentDTO();

    @BeforeEach
    void setUp() {
        departmentsApi = new DepartmentsApi(apiClient);

        sampleDept.setId(101);
        sampleDept.setName("HR Department");
        sampleDept.setCode("HR");
    }

    @Test
    void getDepartmentById_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(DepartmentDTO.class)))
                .thenReturn(sampleDept);

        DepartmentDTO result = departmentsApi.getDepartmentByCode("HR");
        assertEquals("HR Department", result.getName());
        verify(apiClient).invokeAPI("/api/departments/by-code/HR", "GET", null, null, null, DepartmentDTO.class);
    }

    @Test
    void updateDepartment_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("PUT"), isNull(), eq(sampleDept), isNull(), eq(DepartmentDTO.class)))
                .thenReturn(sampleDept);

        DepartmentDTO result = departmentsApi.updateDepartment("HR", sampleDept);
        assertEquals("HR", result.getCode());
    }

    @Test
    void deleteDepartment_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("DELETE"), isNull(), isNull(), isNull(), eq(String.class)))
                .thenReturn("Deleted");

        String result = departmentsApi.deleteDepartment("HR");
        assertEquals("Deleted", result);
    }

    @Test
    void createDepartment_Success() throws ApiException {
        when(apiClient.invokeAPI(eq("/api/departments"), eq("POST"), isNull(), eq(sampleDept), isNull(), eq(DepartmentDTO.class)))
                .thenReturn(sampleDept);

        DepartmentDTO result = departmentsApi.createDepartment(sampleDept);
        assertEquals(101, result.getId());
    }

    @Test
    void getDepartments_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":101,\"name\":\"HR\"},{\"id\":102,\"name\":\"Finance\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<DepartmentDTO> result = departmentsApi.getDepartments(null, null, null, null);
        assertEquals(2, result.size());
        assertEquals("HR", result.getFirst().getName());
    }

    @Test
    void getDepartmentApplications_WithApplicationCodeFilter_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"code\":\"APP001\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/applications"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getDepartmentApplications("HR", "APP001");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/applications",
                "GET",
                Map.of("applicationCode", "APP001"),
                null,
                null
        );
    }

    @Test
    void getDepartmentMenus_WithMenuCodeFilter_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"code\":\"MENU001\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/applications/APP001/menus"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getDepartmentMenus("HR", "APP001", "MENU001");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/applications/APP001/menus",
                "GET",
                Map.of("menuCode", "MENU001"),
                null,
                null
        );
    }

    @Test
    void getDepartmentResources_WithResourceNameFilter_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"name\":\"Resource1\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/resources"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getDepartmentResources("HR", "Resource1");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/resources",
                "GET",
                Map.of("resourceName", "Resource1"),
                null,
                null
        );
    }

    @Test
    void getDepartmentPermissions_WithPermissionNameFilter_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"name\":\"Permission1\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/permissions"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getDepartmentPermissions("HR", "Permission1");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/permissions",
                "GET",
                Map.of("permissionName", "Permission1"),
                null,
                null
        );
    }

    @Test
    void getDepartmentPermissions_WithResourceNameFilter_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"name\":\"Permission1\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/permissions"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getDepartmentPermissions("HR", null, "invoice-service");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/permissions",
                "GET",
                Map.of("resourceName", "invoice-service"),
                null,
                null
        );
    }

    @Test
    void getDepartmentPermissions_WithBothFilters_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"name\":\"Permission1\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/permissions"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getDepartmentPermissions("HR", "Permission1", "invoice-service");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/permissions",
                "GET",
                Map.of("permissionName", "Permission1", "resourceName", "invoice-service"),
                null,
                null
        );
    }

    @Test
    void getAvailablePermissions_WithResourceNameFilter_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":1,\"name\":\"Perm1\"}]";
        when(apiClient.invokeAPIRaw(eq("/api/departments/HR/permissions/available"), eq("GET"), anyMap(), isNull(), isNull()))
                .thenReturn(jsonResponse);

        List<?> result = departmentsApi.getAvailablePermissions("HR", "Resource1");

        assertEquals(1, result.size());
        verify(apiClient).invokeAPIRaw(
                "/api/departments/HR/permissions/available",
                "GET",
                Map.of("resourceName", "Resource1"),
                null,
                null
        );
    }
}