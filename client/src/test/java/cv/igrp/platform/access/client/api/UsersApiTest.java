package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.JwtUtil;
import cv.igrp.platform.access.client.model.IGRPUserDTO;
import cv.igrp.platform.access.client.model.RoleDTO;
import cv.igrp.platform.access.client.model.RoleUserDTO;
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
class UsersApiTest {

    @Mock
    private IApiClient apiClient;

    private UsersApi usersApi;
    private String jwt;
    private final IGRPUserDTO sampleUser = new IGRPUserDTO();

    @BeforeEach
    void setUp() {
        usersApi = new UsersApi(apiClient);

        sampleUser.setId(601);
        sampleUser.setUsername("john.doe");
        sampleUser.setName("John Doe");
        sampleUser.setEmail("john@example.com");

        jwt = "";

    }

    @Test
    void getUser_Success() throws ApiException {
        when(apiClient.invokeAPI(anyString(), eq("GET"), isNull(), isNull(), isNull(), eq(IGRPUserDTO.class)))
                .thenReturn(sampleUser);

        IGRPUserDTO result = usersApi.getUser("john.doe");
        assertEquals("John Doe", result.getName());
    }

    @Test
    void createUser_Success() throws ApiException {
        when(apiClient.invokeAPI(eq("/api/users"), eq("POST"), isNull(), eq(sampleUser), isNull(), eq(IGRPUserDTO.class)))
                .thenReturn(sampleUser);

        IGRPUserDTO result = usersApi.createUser(sampleUser);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getCurrentUser_Success() throws ApiException {
        when(apiClient.invokeAPI(eq("/api/users/me"), eq("GET"), isNull(), isNull(), isNull(), eq(IGRPUserDTO.class)))
                .thenReturn(sampleUser);

        IGRPUserDTO result = usersApi.getCurrentUser();
        assertNotNull(result);
    }

    @Test
    void addRolesToUser_Success() throws ApiException {
        RoleDTO role = new RoleDTO();
        role.setId(701);
        role.setName("admin");
        when(apiClient.invokeAPI(anyString(), eq("POST"), isNull(), any(List.class), isNull(), eq(RoleDTO.class)))
                .thenReturn(role);

        RoleDTO result = usersApi.addRolesToUser("john.doe", List.of("admin"));
        assertEquals(701, result.getId());
    }

    @Test
    void getUsers_Success() throws ApiException, IOException, InterruptedException {
        String jsonResponse = "[{\"id\":601,\"name\":\"User1\"},{\"id\":602,\"name\":\"User2\"}]";

        Map<String, String> expectedParams = Map.of(
                "departmentCode", "HR",
                "name", "John",
                "applicationCode", "APP",
                "email", "john@example.com"
        );

        when(apiClient.invokeAPIRaw(
                eq("/api/users"),
                eq("GET"),
                eq(expectedParams),
                isNull(),
                isNull()
        )).thenReturn(jsonResponse);

        List<IGRPUserDTO> result = usersApi.getUsers(
                "APP", "HR", "John", "john@example.com"
        );

        assertEquals(2, result.size());
        assertEquals(601, result.get(0).getId());
        assertEquals(602, result.get(1).getId());
    }

    /*@Test
    void getToken() {

        boolean result = JwtUtil.hasSuperAdminRole(jwt);

        assertTrue(result);

    }*/

}