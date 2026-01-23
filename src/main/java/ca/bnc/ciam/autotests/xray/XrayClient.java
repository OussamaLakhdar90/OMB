package ca.bnc.ciam.autotests.xray;

import ca.bnc.ciam.autotests.api.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for Xray Cloud API.
 * Handles authentication and API requests.
 */
@Slf4j
public class XrayClient {

    private static final String AUTH_ENDPOINT = "/authenticate";
    private static final String IMPORT_EXECUTION_ENDPOINT = "/import/execution";
    private static final String GRAPHQL_ENDPOINT = "/graphql";

    private final XrayConfig config;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private String authToken;

    /**
     * Create client with configuration.
     */
    public XrayClient(XrayConfig config) {
        this.config = config;
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .timeout(config.getConnectionTimeout())
                .logging(false) // Disable logging for security
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Authenticate with Xray Cloud API.
     */
    public void authenticate() {
        if (!config.isEnabled() || !config.isValid()) {
            log.warn("Xray is disabled or configuration is invalid. Skipping authentication.");
            return;
        }

        try {
            ObjectNode authRequest = objectMapper.createObjectNode();
            authRequest.put("client_id", config.getClientId());
            authRequest.put("client_secret", config.getClientSecret());

            Response response = restClient.post(AUTH_ENDPOINT, authRequest.toString());

            if (response.getStatusCode() == 200) {
                // Token is returned as plain text
                authToken = response.getBody().asString().replace("\"", "");
                log.info("Successfully authenticated with Xray Cloud");
            } else {
                log.error("Failed to authenticate with Xray: {} - {}",
                        response.getStatusCode(), response.getBody().asString());
            }
        } catch (Exception e) {
            log.error("Error authenticating with Xray", e);
        }
    }

    /**
     * Check if authenticated.
     */
    public boolean isAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    /**
     * Import test execution results.
     */
    public String importExecution(XrayTestExecution execution) {
        if (!isAuthenticated()) {
            authenticate();
        }

        if (!isAuthenticated()) {
            log.error("Cannot import execution: not authenticated");
            return null;
        }

        try {
            String payload = execution.toJson();
            log.debug("Importing execution: {}", payload);

            Response response = restClient.given()
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .post(IMPORT_EXECUTION_ENDPOINT);

            if (response.getStatusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.getBody().asString());
                String executionKey = responseJson.path("key").asText();
                log.info("Successfully imported test execution: {}", executionKey);
                return executionKey;
            } else {
                log.error("Failed to import execution: {} - {}",
                        response.getStatusCode(), response.getBody().asString());
                return null;
            }
        } catch (Exception e) {
            log.error("Error importing execution", e);
            return null;
        }
    }

    /**
     * Get test execution details.
     */
    public JsonNode getTestExecution(String executionKey) {
        if (!isAuthenticated()) {
            authenticate();
        }

        if (!isAuthenticated()) {
            log.error("Cannot get execution: not authenticated");
            return null;
        }

        try {
            String query = String.format("""
                {
                  getTestExecution(issueId: "%s") {
                    issueId
                    jira(fields: ["key", "summary"])
                    testRuns(limit: 100) {
                      results {
                        id
                        status {
                          name
                        }
                        test {
                          issueId
                          jira(fields: ["key", "summary"])
                        }
                      }
                    }
                  }
                }
                """, executionKey);

            ObjectNode graphqlRequest = objectMapper.createObjectNode();
            graphqlRequest.put("query", query);

            Response response = restClient.given()
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .body(graphqlRequest.toString())
                    .post(GRAPHQL_ENDPOINT);

            if (response.getStatusCode() == 200) {
                return objectMapper.readTree(response.getBody().asString());
            } else {
                log.error("Failed to get execution: {} - {}",
                        response.getStatusCode(), response.getBody().asString());
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting execution", e);
            return null;
        }
    }

    /**
     * Create test execution.
     */
    public String createTestExecution(String summary, List<String> testKeys) {
        if (!isAuthenticated()) {
            authenticate();
        }

        if (!isAuthenticated()) {
            log.error("Cannot create execution: not authenticated");
            return null;
        }

        try {
            String testsJson = testKeys.stream()
                    .map(k -> "\"" + k + "\"")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            String mutation = String.format("""
                mutation {
                  createTestExecution(
                    testIssueIds: [%s],
                    jira: {
                      fields: {
                        summary: "%s",
                        project: { key: "%s" }
                      }
                    }
                  ) {
                    testExecution {
                      issueId
                      jira(fields: ["key"])
                    }
                  }
                }
                """, testsJson, summary, config.getProjectKey());

            ObjectNode graphqlRequest = objectMapper.createObjectNode();
            graphqlRequest.put("query", mutation);

            Response response = restClient.given()
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .body(graphqlRequest.toString())
                    .post(GRAPHQL_ENDPOINT);

            if (response.getStatusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.getBody().asString());
                String key = responseJson.path("data")
                        .path("createTestExecution")
                        .path("testExecution")
                        .path("jira")
                        .path("key")
                        .asText();
                log.info("Created test execution: {}", key);
                return key;
            } else {
                log.error("Failed to create execution: {} - {}",
                        response.getStatusCode(), response.getBody().asString());
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating execution", e);
            return null;
        }
    }

    /**
     * Update test run status.
     */
    public boolean updateTestRunStatus(String testRunId, String status) {
        if (!isAuthenticated()) {
            authenticate();
        }

        if (!isAuthenticated()) {
            log.error("Cannot update test run: not authenticated");
            return false;
        }

        try {
            String mutation = String.format("""
                mutation {
                  updateTestRunStatus(
                    id: "%s",
                    status: "%s"
                  )
                }
                """, testRunId, status);

            ObjectNode graphqlRequest = objectMapper.createObjectNode();
            graphqlRequest.put("query", mutation);

            Response response = restClient.given()
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .body(graphqlRequest.toString())
                    .post(GRAPHQL_ENDPOINT);

            if (response.getStatusCode() == 200) {
                log.info("Updated test run {} status to {}", testRunId, status);
                return true;
            } else {
                log.error("Failed to update test run: {} - {}",
                        response.getStatusCode(), response.getBody().asString());
                return false;
            }
        } catch (Exception e) {
            log.error("Error updating test run", e);
            return false;
        }
    }

    /**
     * Add evidence (attachment) to test run.
     */
    public boolean addEvidence(String testRunId, String filename, byte[] data, String contentType) {
        if (!isAuthenticated()) {
            authenticate();
        }

        if (!isAuthenticated()) {
            log.error("Cannot add evidence: not authenticated");
            return false;
        }

        try {
            String base64Data = java.util.Base64.getEncoder().encodeToString(data);

            String mutation = String.format("""
                mutation {
                  addEvidenceToTestRun(
                    id: "%s",
                    evidence: [{
                      filename: "%s",
                      mimeType: "%s",
                      data: "%s"
                    }]
                  )
                }
                """, testRunId, filename, contentType, base64Data);

            ObjectNode graphqlRequest = objectMapper.createObjectNode();
            graphqlRequest.put("query", mutation);

            Response response = restClient.given()
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .body(graphqlRequest.toString())
                    .post(GRAPHQL_ENDPOINT);

            if (response.getStatusCode() == 200) {
                log.info("Added evidence {} to test run {}", filename, testRunId);
                return true;
            } else {
                log.error("Failed to add evidence: {} - {}",
                        response.getStatusCode(), response.getBody().asString());
                return false;
            }
        } catch (Exception e) {
            log.error("Error adding evidence", e);
            return false;
        }
    }

    /**
     * Get configuration.
     */
    public XrayConfig getConfig() {
        return config;
    }
}
