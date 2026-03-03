package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.RoleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolesApiTest {

    @Mock
    private IApiClient apiClient;

    private RolesApi rolesApi;
    private final RoleDTO sampleRole = new RoleDTO();

    @BeforeEach
    void setUp() {
        rolesApi = new RolesApi(apiClient);

        sampleRole.setId(501);
        sampleRole.setCode("admin");
    }

    @Test
    void getRoleById_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(RoleDTO.class)))
                .thenReturn(sampleRole);

        RoleDTO result = rolesApi.getRoleByCode("admin");
        assertEquals("admin", result.getCode());
    }

}