package cv.igrp.platform.access.client.api;

import cv.igrp.platform.access.client.ApiClient;
import cv.igrp.platform.access.client.ApiException;
import cv.igrp.platform.access.client.IApiClient;
import cv.igrp.platform.access.client.model.FileRequestDTO;
import cv.igrp.platform.access.client.model.FileUrlDTO;

import java.util.HashMap;
import java.util.Map;

public class FilesApi {
    private IApiClient apiClient;

    public FilesApi() { this(new ApiClient()); }

    public FilesApi(IApiClient apiClient) { this.apiClient = apiClient; }

    public IApiClient getApiClient() { return apiClient; }

    public void setApiClient(IApiClient apiClient) {this.apiClient = apiClient; }

    public FileUrlDTO getPrivateFilesUrl(String filePath) throws ApiException {
        if (filePath == null) {
            throw new ApiException(400, "Missing the required parameter 'filePath'");
        }
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("filePath", filePath);

        String path = "/api/files/url";

        return apiClient.invokeAPI(
                path,
                "GET",
                queryParams,
                null,
                null,
                FileUrlDTO.class
        );
    }

    public FileUrlDTO uploadPublicFile(String folder, FileRequestDTO file) throws ApiException {

        if (folder == null) {
            throw new ApiException(400, "Missing the required parameter 'folder'");
        }

        if (file == null) {
            throw new ApiException(400, "Missing the required parameter 'file'");
        }

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("folder", folder);

        Map<String, String> headerParams = new HashMap<>();
        headerParams.put("Content-Type", "multipart/form-data");

        String path = "/api/files/public";

        return apiClient.invokeAPI(
                path,
                "POST",
                queryParams,
                file,
                headerParams,
                FileUrlDTO.class

        );
    }

    private FileUrlDTO uploadPrivateFile(String folder, FileRequestDTO file) throws ApiException {

        if (folder == null) {
            throw new ApiException(400, "Missing the required parameter 'folder'");
        }

        if (file == null) {
            throw new ApiException(400, "Missing the required parameter 'file'");
        }

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("folder", folder);

        Map<String, String> headerParams = new HashMap<>();
        headerParams.put("Content-Type", "multipart/form-data");

        String path = "/api/files/private";

        return apiClient.invokeAPI(
                path,
                "POST",
                queryParams,
                file,
                headerParams,
                FileUrlDTO.class
        );
    }
}














