package ca.bnc.ciam.autotests.api;

import ca.bnc.ciam.autotests.base.AbstractDataDrivenTest;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Generic API test utilities that can be used across different API endpoints.
 * Extends AbstractDataDrivenTest to access the World context.
 */
@Slf4j
public class ApiGeneric extends AbstractDataDrivenTest {

    protected RestClient client;

    /**
     * Initialize with a base URL.
     */
    public ApiGeneric(String baseUrl) {
        this.client = RestClient.forBaseUrl(baseUrl);
    }

    /**
     * Initialize with a RestClient.
     */
    public ApiGeneric(RestClient client) {
        this.client = client;
    }

    /**
     * Get the underlying REST client.
     */
    public RestClient getClient() {
        return client;
    }

    /**
     * Perform GET and wrap response.
     */
    public ApiResponse doGet(String path) {
        Response response = client.get(path);
        return new ApiResponse(response);
    }

    /**
     * Perform GET with query parameters and wrap response.
     */
    public ApiResponse doGet(String path, Map<String, ?> queryParams) {
        Response response = client.getWithQueryParams(path, queryParams);
        return new ApiResponse(response);
    }

    /**
     * Perform POST with body and wrap response.
     */
    public ApiResponse doPost(String path, Object body) {
        Response response = client.post(path, body);
        return new ApiResponse(response);
    }

    /**
     * Perform POST without body and wrap response.
     */
    public ApiResponse doPost(String path) {
        Response response = client.post(path);
        return new ApiResponse(response);
    }

    /**
     * Perform PUT with body and wrap response.
     */
    public ApiResponse doPut(String path, Object body) {
        Response response = client.put(path, body);
        return new ApiResponse(response);
    }

    /**
     * Perform PATCH with body and wrap response.
     */
    public ApiResponse doPatch(String path, Object body) {
        Response response = client.patch(path, body);
        return new ApiResponse(response);
    }

    /**
     * Perform DELETE and wrap response.
     */
    public ApiResponse doDelete(String path) {
        Response response = client.delete(path);
        return new ApiResponse(response);
    }

    /**
     * Perform DELETE with path parameters and wrap response.
     */
    public ApiResponse doDelete(String path, Map<String, ?> pathParams) {
        Response response = client.delete(path, pathParams);
        return new ApiResponse(response);
    }

    /**
     * Store response in World context for later use.
     */
    public void storeResponse(String key, ApiResponse response) {
        pushToTheWorld(WorldKey.valueOf(key), response);
    }

    /**
     * Retrieve response from World context.
     */
    public ApiResponse getStoredResponse(String key) {
        return pullFromTheWorld(WorldKey.valueOf(key), ApiResponse.class);
    }

    /**
     * Validate response status code.
     */
    public boolean validateStatus(ApiResponse response, int expectedStatus) {
        boolean valid = response.getStatusCode() == expectedStatus;
        if (!valid) {
            log.error("Status validation failed. Expected: {}, Actual: {}",
                    expectedStatus, response.getStatusCode());
        }
        return valid;
    }

    /**
     * Validate response status code using HttpStatus enum.
     */
    public boolean validateStatus(ApiResponse response, HttpStatus expectedStatus) {
        return validateStatus(response, expectedStatus.getCode());
    }

    /**
     * Validate that response is successful (2xx).
     */
    public boolean validateSuccess(ApiResponse response) {
        boolean valid = response.isSuccess();
        if (!valid) {
            log.error("Response is not successful. Status: {}", response.getStatusCode());
        }
        return valid;
    }

    /**
     * Extract and store a value from response using JsonPath.
     */
    public <T> T extractAndStore(ApiResponse response, String jsonPath, String worldKey) {
        T value = response.jsonPath(jsonPath);
        pushToTheWorld(WorldKey.valueOf(worldKey), value);
        log.info("Extracted and stored [{}] = {} from path [{}]", worldKey, value, jsonPath);
        return value;
    }

    /**
     * Log response details for debugging.
     */
    public void logResponse(ApiResponse response) {
        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Time: {} ms", response.getResponseTime());
        log.debug("Response Body: {}", response.getBody());
    }

    /**
     * Assert response status and return response for chaining.
     */
    public ApiResponse assertStatusAndReturn(ApiResponse response, HttpStatus expectedStatus) {
        response.assertStatus(expectedStatus);
        return response;
    }
}
