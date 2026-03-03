package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.FileRequestDTO;
import cv.igrp.platform.access.client.model.FileUrlDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilesApiTest {

    @Mock
    private IApiClient apiClient;

    @InjectMocks
    private FilesApi filesApi;

    private FileUrlDTO mockFileUrl;
    private FileRequestDTO mockFileRequest;

    @BeforeEach
    void setUp() {
        mockFileUrl = new FileUrlDTO();
        mockFileUrl.setUrl("https://example.com/file.txt");

        mockFileRequest = new FileRequestDTO();
        // Set up mock file request properties as needed
    }

    @Test
    void getPrivateFilesUrl_shouldReturnFileUrl_whenValidPathProvided() throws ApiException {
        // Arrange
        String testPath = "documents/test.txt";
        when(apiClient.invokeAPI(
                eq("/api/files/url"),
                eq("GET"),
                anyMap(),
                isNull(),
                isNull(),
                eq(FileUrlDTO.class)
        )).thenReturn(mockFileUrl);

        // Act
        FileUrlDTO result = filesApi.getPrivateFilesUrl(testPath);

        // Assert
        assertNotNull(result);
        assertEquals("https://example.com/file.txt", result.getUrl());
        verify(apiClient).invokeAPI(
                eq("/api/files/url"),
                eq("GET"),
                argThat(map -> map.get("filePath").equals(testPath)),
                isNull(),
                isNull(),
                eq(FileUrlDTO.class)
        );
    }

    @Test
    void getPrivateFilesUrl_shouldThrowApiException_whenPathIsNull() {
        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> filesApi.getPrivateFilesUrl(null));

        assertEquals(400, exception.getCode());
        assertEquals("Missing the required parameter 'filePath'", exception.getMessage());
    }

    @Test
    void uploadPublicFile_shouldReturnFileUrl_whenValidInput() throws ApiException {
        // Arrange
        String testFolder = "uploads";
        when(apiClient.invokeAPI(
                eq("/api/files/public"),
                eq("POST"),
                anyMap(),
                eq(mockFileRequest),
                anyMap(),
                eq(FileUrlDTO.class)
        )).thenReturn(mockFileUrl);

        // Act
        FileUrlDTO result = filesApi.uploadPublicFile(testFolder, mockFileRequest);

        // Assert
        assertNotNull(result);
        assertEquals("https://example.com/file.txt", result.getUrl());
        verify(apiClient).invokeAPI(
                eq("/api/files/public"),
                eq("POST"),
                argThat(map -> map.get("folder").equals(testFolder)),
                eq(mockFileRequest),
                argThat(headers -> headers.get("Content-Type").equals("multipart/form-data")),
                eq(FileUrlDTO.class)
        );
    }

    @Test
    void uploadPublicFile_shouldThrowApiException_whenFolderIsNull() {
        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> filesApi.uploadPublicFile(null, mockFileRequest));

        assertEquals(400, exception.getCode());
        assertEquals("Missing the required parameter 'folder'", exception.getMessage());
    }

    @Test
    void uploadPublicFile_shouldThrowApiException_whenFileIsNull() {
        // Act & Assert
        ApiException exception = assertThrows(ApiException.class,
                () -> filesApi.uploadPublicFile("uploads", null));

        assertEquals(400, exception.getCode());
        assertEquals("Missing the required parameter 'file'", exception.getMessage());
    }

    @Test
    void setApiClient_shouldUpdateApiClient() {
        // Arrange
        IApiClient newClient = mock(IApiClient.class);

        // Act
        filesApi.setApiClient(newClient);

        // Assert
        assertEquals(newClient, filesApi.getApiClient());
    }

    // Note: uploadPrivateFile is private, so we can't test it directly
    // It would be tested indirectly through any public methods that use it
}