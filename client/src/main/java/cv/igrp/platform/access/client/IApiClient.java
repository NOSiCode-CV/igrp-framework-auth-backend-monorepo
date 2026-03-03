package cv.igrp.platform.access.client;

import java.io.IOException;
import java.util.Map;

// [marcelo.monteiro - 2025-06-26 14:54:23] TODO: Move this to a core library for API clients SDKs
public interface IApiClient {
    <T> T invokeAPI(String path,
                    String method,
                    Map<String, String> queryParams,
                    Object body,
                    Map<String, String> headerParams,
                    Class<T> returnType) throws ApiException;

    String invokeAPIRaw(String path,
                        String method,
                        Map<String, String> queryParams,
                        Object body,
                        Map<String, String> headerParams) throws ApiException, IOException, InterruptedException;

    String getToken();

}
