package ca.bnc.ciam.autotests.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Wrapper for API responses providing convenient access methods.
 */
@Slf4j
public class ApiResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Response response;
    private final long responseTime;
    private final int statusCode;
    private final String body;

    public ApiResponse(Response response) {
        this.response = response;
        this.responseTime = response.getTime();
        this.statusCode = response.getStatusCode();
        this.body = response.getBody().asString();
    }

    /**
     * Get the raw RestAssured response.
     */
    public Response getRawResponse() {
        return response;
    }

    /**
     * Get the HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the HTTP status as enum.
     */
    public HttpStatus getStatus() {
        return HttpStatus.fromCode(statusCode);
    }

    /**
     * Get the response time in milliseconds.
     */
    public long getResponseTime() {
        return responseTime;
    }

    /**
     * Get the response body as string.
     */
    public String getBody() {
        return body;
    }

    /**
     * Get the response body as a specific type.
     */
    public <T> T getBodyAs(Class<T> type) {
        return response.getBody().as(type);
    }

    /**
     * Get the response body as a specific type using TypeReference.
     */
    public <T> T getBodyAs(TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(body, typeReference);
        } catch (Exception e) {
            log.error("Failed to deserialize response body", e);
            throw new RuntimeException("Failed to deserialize response body", e);
        }
    }

    /**
     * Get the response body as JsonNode for flexible access.
     */
    public JsonNode getBodyAsJson() {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("Failed to parse response body as JSON", e);
            throw new RuntimeException("Failed to parse response body as JSON", e);
        }
    }

    /**
     * Get the response body as a Map.
     */
    public Map<String, Object> getBodyAsMap() {
        return getBodyAs(new TypeReference<>() {});
    }

    /**
     * Get the response body as a List of Maps.
     */
    public List<Map<String, Object>> getBodyAsList() {
        return getBodyAs(new TypeReference<>() {});
    }

    /**
     * Extract a value using JsonPath.
     */
    public <T> T jsonPath(String path) {
        return response.jsonPath().get(path);
    }

    /**
     * Extract a String value using JsonPath.
     */
    public String jsonPathString(String path) {
        return response.jsonPath().getString(path);
    }

    /**
     * Extract an Integer value using JsonPath.
     */
    public Integer jsonPathInt(String path) {
        return response.jsonPath().getInt(path);
    }

    /**
     * Extract a List using JsonPath.
     */
    public <T> List<T> jsonPathList(String path) {
        return response.jsonPath().getList(path);
    }

    /**
     * Get a response header value.
     */
    public String getHeader(String name) {
        return response.getHeader(name);
    }

    /**
     * Get all response headers.
     */
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new java.util.HashMap<>();
        response.getHeaders().forEach(h -> headers.put(h.getName(), h.getValue()));
        return headers;
    }

    /**
     * Get a cookie value.
     */
    public String getCookie(String name) {
        return response.getCookie(name);
    }

    /**
     * Get all cookies.
     */
    public Map<String, String> getCookies() {
        return response.getCookies();
    }

    /**
     * Check if the response was successful (2xx).
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Check if the response indicates a client error (4xx).
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Check if the response indicates a server error (5xx).
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Assert that the status code matches expected.
     */
    public ApiResponse assertStatus(int expectedStatus) {
        if (statusCode != expectedStatus) {
            throw new AssertionError(String.format(
                    "Expected status %d but got %d. Response body: %s",
                    expectedStatus, statusCode, body));
        }
        return this;
    }

    /**
     * Assert that the status code matches expected HttpStatus.
     */
    public ApiResponse assertStatus(HttpStatus expectedStatus) {
        return assertStatus(expectedStatus.getCode());
    }

    /**
     * Assert that the response is successful (2xx).
     */
    public ApiResponse assertSuccess() {
        if (!isSuccess()) {
            throw new AssertionError(String.format(
                    "Expected successful response but got %d. Response body: %s",
                    statusCode, body));
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("ApiResponse[status=%d, time=%dms, bodyLength=%d]",
                statusCode, responseTime, body.length());
    }
}
