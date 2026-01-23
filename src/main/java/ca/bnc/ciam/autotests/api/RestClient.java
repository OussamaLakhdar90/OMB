package ca.bnc.ciam.autotests.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API client built on RestAssured.
 * Provides a fluent interface for making HTTP requests.
 */
@Slf4j
public class RestClient {

    private static final int DEFAULT_TIMEOUT = 30000;

    private final String baseUrl;
    private final Map<String, String> defaultHeaders;
    private final int timeout;
    private final boolean enableLogging;
    private RequestSpecification requestSpec;

    private RestClient(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.defaultHeaders = builder.defaultHeaders;
        this.timeout = builder.timeout;
        this.enableLogging = builder.enableLogging;
        this.requestSpec = buildRequestSpec();
    }

    /**
     * Create a new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a simple client with just a base URL.
     */
    public static RestClient forBaseUrl(String baseUrl) {
        return builder().baseUrl(baseUrl).build();
    }

    /**
     * Build the base request specification.
     */
    private RequestSpecification buildRequestSpec() {
        RequestSpecBuilder specBuilder = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", timeout)
                                .setParam("http.socket.timeout", timeout)));

        // Add default headers
        defaultHeaders.forEach(specBuilder::addHeader);

        // Add logging filters if enabled
        if (enableLogging) {
            specBuilder.addFilter(new RequestLoggingFilter(LogDetail.ALL));
            specBuilder.addFilter(new ResponseLoggingFilter(LogDetail.ALL));
        }

        return specBuilder.build();
    }

    /**
     * Get a fresh request specification for a new request.
     */
    public RequestSpecification given() {
        return RestAssured.given().spec(requestSpec);
    }

    /**
     * Perform a GET request.
     */
    public Response get(String path) {
        log.debug("GET {}{}", baseUrl, path);
        return given().get(path);
    }

    /**
     * Perform a GET request with path parameters.
     */
    public Response get(String path, Map<String, ?> pathParams) {
        log.debug("GET {}{} with params: {}", baseUrl, path, pathParams);
        return given().pathParams(pathParams).get(path);
    }

    /**
     * Perform a GET request with query parameters.
     */
    public Response getWithQueryParams(String path, Map<String, ?> queryParams) {
        log.debug("GET {}{} with query: {}", baseUrl, path, queryParams);
        return given().queryParams(queryParams).get(path);
    }

    /**
     * Perform a POST request with JSON body.
     */
    public Response post(String path, Object body) {
        log.debug("POST {}{}", baseUrl, path);
        return given().body(body).post(path);
    }

    /**
     * Perform a POST request without body.
     */
    public Response post(String path) {
        log.debug("POST {}{}", baseUrl, path);
        return given().post(path);
    }

    /**
     * Perform a PUT request with JSON body.
     */
    public Response put(String path, Object body) {
        log.debug("PUT {}{}", baseUrl, path);
        return given().body(body).put(path);
    }

    /**
     * Perform a PATCH request with JSON body.
     */
    public Response patch(String path, Object body) {
        log.debug("PATCH {}{}", baseUrl, path);
        return given().body(body).patch(path);
    }

    /**
     * Perform a DELETE request.
     */
    public Response delete(String path) {
        log.debug("DELETE {}{}", baseUrl, path);
        return given().delete(path);
    }

    /**
     * Perform a DELETE request with path parameters.
     */
    public Response delete(String path, Map<String, ?> pathParams) {
        log.debug("DELETE {}{} with params: {}", baseUrl, path, pathParams);
        return given().pathParams(pathParams).delete(path);
    }

    /**
     * Add an authorization header for this request.
     */
    public RestClient withAuth(String token) {
        this.requestSpec = given().header("Authorization", "Bearer " + token).spec(requestSpec);
        return this;
    }

    /**
     * Add a custom header for subsequent requests.
     */
    public RestClient withHeader(String name, String value) {
        this.requestSpec = given().header(name, value).spec(requestSpec);
        return this;
    }

    /**
     * Get the base URL.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Builder for RestClient.
     */
    public static class Builder {
        private String baseUrl = "";
        private Map<String, String> defaultHeaders = new HashMap<>();
        private int timeout = DEFAULT_TIMEOUT;
        private boolean enableLogging = true;

        /**
         * Set the base URL for all requests.
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Add a default header to all requests.
         */
        public Builder header(String name, String value) {
            this.defaultHeaders.put(name, value);
            return this;
        }

        /**
         * Set all default headers.
         */
        public Builder headers(Map<String, String> headers) {
            this.defaultHeaders.putAll(headers);
            return this;
        }

        /**
         * Set the connection timeout in milliseconds.
         */
        public Builder timeout(int timeoutMs) {
            this.timeout = timeoutMs;
            return this;
        }

        /**
         * Enable or disable request/response logging.
         */
        public Builder logging(boolean enabled) {
            this.enableLogging = enabled;
            return this;
        }

        /**
         * Build the RestClient instance.
         */
        public RestClient build() {
            return new RestClient(this);
        }
    }
}
