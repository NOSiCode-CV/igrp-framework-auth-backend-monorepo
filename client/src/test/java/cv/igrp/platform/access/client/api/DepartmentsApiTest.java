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
}