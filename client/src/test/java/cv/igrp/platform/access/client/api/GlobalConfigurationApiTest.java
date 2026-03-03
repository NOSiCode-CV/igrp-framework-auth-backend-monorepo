package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.GlobalConfigurationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalConfigurationApiTest {

    @Mock
    private IApiClient apiClient;

    private GlobalConfigurationApi configApi;
    private final GlobalConfigurationDTO sampleConfig = new GlobalConfigurationDTO();

    @BeforeEach
    void setUp() {
        configApi = new GlobalConfigurationApi(apiClient);

        sampleConfig.setType("CLUSTER");
        sampleConfig.setConfig("{ \"setting\": \"value\" }");
    }

    @Test
    void getGlobalConfiguration_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), anyMap(), isNull(), isNull(), eq(GlobalConfigurationDTO.class)))
                .thenReturn(sampleConfig);

        GlobalConfigurationDTO result = configApi.getGlobalConfiguration("CLUSTER");
        assertEquals("{ \"setting\": \"value\" }", result.getConfig());
        verify(apiClient).invokeAPI("/api/global-configuration", "GET", Map.of("type", "CLUSTER"), null, null, GlobalConfigurationDTO.class);
    }

    @Test
    void setGlobalConfiguration_Success() throws ApiException {
        when(apiClient.invokeAPI(eq("/api/global-configuration"), eq("POST"), isNull(), eq(sampleConfig), isNull(), eq(GlobalConfigurationDTO.class)))
                .thenReturn(sampleConfig);

        GlobalConfigurationDTO result = configApi.setGlobalConfiguration(sampleConfig);
        assertEquals("CLUSTER", result.getType());
    }
}