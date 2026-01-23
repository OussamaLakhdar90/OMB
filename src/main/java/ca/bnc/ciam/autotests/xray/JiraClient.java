package ca.bnc.ciam.autotests.xray;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP client for Jira REST API.
 * Handles adding comments, updating issues, etc.
 *
 * Supports both Jira Cloud (ADF format) and Jira Server (Wiki Markup).
 */
@Slf4j
public class JiraClient {

    private final String baseUrl;
    private final String username;
    private final String apiToken;
    private final boolean isCloud;
    private final ObjectMapper objectMapper;

    /**
     * Create a Jira client.
     *
     * @param baseUrl   Jira base URL (e.g., https://company.atlassian.net)
     * @param username  Jira username (email for Cloud)
     * @param apiToken  API token (Cloud) or password (Server)
     * @param isCloud   true for Jira Cloud, false for Server/Data Center
     */
    @Builder
    public JiraClient(String baseUrl, String username, String apiToken, boolean isCloud) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.apiToken = apiToken;
        this.isCloud = isCloud;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a Jira Cloud client from environment variables.
     */
    public static JiraClient fromEnvironment() {
        return JiraClient.builder()
                .baseUrl(getEnvOrProperty("JIRA_BASE_URL", "jira.base.url"))
                .username(getEnvOrProperty("JIRA_USERNAME", "jira.username"))
                .apiToken(getEnvOrProperty("JIRA_API_TOKEN", "jira.api.token"))
                .isCloud(Boolean.parseBoolean(getEnvOrProperty("JIRA_IS_CLOUD", "jira.is.cloud", "true")))
                .build();
    }

    /**
     * Add a comment to a Jira issue.
     *
     * @param issueKey     Issue key (e.g., PROJ-123)
     * @param commentBody  Comment body in Wiki Markup format
     * @return true if successful
     */
    public boolean addComment(String issueKey, String commentBody) {
        if (!isValid()) {
            log.error("Jira client is not configured properly");
            return false;
        }

        try {
            String endpoint = String.format("%s/rest/api/3/issue/%s/comment", baseUrl, issueKey);

            ObjectNode requestBody;
            if (isCloud) {
                // Jira Cloud uses ADF (Atlassian Document Format)
                requestBody = createAdfComment(commentBody);
            } else {
                // Jira Server uses plain body
                requestBody = objectMapper.createObjectNode();
                requestBody.put("body", commentBody);
            }

            Response response = createRequest()
                    .body(requestBody.toString())
                    .post(endpoint);

            if (response.getStatusCode() == 201) {
                log.info("Successfully added comment to issue {}", issueKey);
                return true;
            } else {
                log.error("Failed to add comment to {}: {} - {}",
                        issueKey, response.getStatusCode(), response.getBody().asString());
                return false;
            }
        } catch (Exception e) {
            log.error("Error adding comment to issue {}", issueKey, e);
            return false;
        }
    }

    /**
     * Add a comment with Wiki Markup to a Jira issue (works for both Cloud and Server).
     * For Cloud, converts Wiki Markup to ADF automatically.
     *
     * @param issueKey     Issue key (e.g., PROJ-123)
     * @param wikiMarkup   Comment body in Wiki Markup format
     * @return true if successful
     */
    public boolean addWikiMarkupComment(String issueKey, String wikiMarkup) {
        if (!isValid()) {
            log.error("Jira client is not configured properly");
            return false;
        }

        try {
            String endpoint;
            ObjectNode requestBody;

            if (isCloud) {
                // For Cloud, use the wiki-to-adf conversion or use the internal renderer
                // Option 1: Use the v2 API which still supports wiki markup
                endpoint = String.format("%s/rest/api/2/issue/%s/comment", baseUrl, issueKey);
                requestBody = objectMapper.createObjectNode();
                requestBody.put("body", wikiMarkup);
            } else {
                // Jira Server supports wiki markup directly
                endpoint = String.format("%s/rest/api/2/issue/%s/comment", baseUrl, issueKey);
                requestBody = objectMapper.createObjectNode();
                requestBody.put("body", wikiMarkup);
            }

            Response response = createRequest()
                    .body(requestBody.toString())
                    .post(endpoint);

            if (response.getStatusCode() == 201) {
                log.info("Successfully added wiki markup comment to issue {}", issueKey);
                return true;
            } else {
                log.error("Failed to add comment to {}: {} - {}",
                        issueKey, response.getStatusCode(), response.getBody().asString());
                return false;
            }
        } catch (Exception e) {
            log.error("Error adding comment to issue {}", issueKey, e);
            return false;
        }
    }

    /**
     * Update issue description.
     *
     * @param issueKey    Issue key
     * @param description New description in Wiki Markup
     * @return true if successful
     */
    public boolean updateDescription(String issueKey, String description) {
        if (!isValid()) {
            log.error("Jira client is not configured properly");
            return false;
        }

        try {
            String endpoint = String.format("%s/rest/api/2/issue/%s", baseUrl, issueKey);

            ObjectNode fields = objectMapper.createObjectNode();
            fields.put("description", description);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.set("fields", fields);

            Response response = createRequest()
                    .body(requestBody.toString())
                    .put(endpoint);

            if (response.getStatusCode() == 204) {
                log.info("Successfully updated description for issue {}", issueKey);
                return true;
            } else {
                log.error("Failed to update description for {}: {} - {}",
                        issueKey, response.getStatusCode(), response.getBody().asString());
                return false;
            }
        } catch (Exception e) {
            log.error("Error updating description for issue {}", issueKey, e);
            return false;
        }
    }

    /**
     * Get issue details.
     *
     * @param issueKey Issue key
     * @return Issue JSON or null if failed
     */
    public JsonNode getIssue(String issueKey) {
        if (!isValid()) {
            log.error("Jira client is not configured properly");
            return null;
        }

        try {
            String endpoint = String.format("%s/rest/api/2/issue/%s", baseUrl, issueKey);

            Response response = createRequest()
                    .get(endpoint);

            if (response.getStatusCode() == 200) {
                return objectMapper.readTree(response.getBody().asString());
            } else {
                log.error("Failed to get issue {}: {} - {}",
                        issueKey, response.getStatusCode(), response.getBody().asString());
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting issue {}", issueKey, e);
            return null;
        }
    }

    /**
     * Check if configuration is valid.
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.isEmpty() &&
                username != null && !username.isEmpty() &&
                apiToken != null && !apiToken.isEmpty();
    }

    /**
     * Create authenticated request.
     */
    private RequestSpecification createRequest() {
        return RestAssured.given()
                .auth().preemptive().basic(username, apiToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    /**
     * Create ADF (Atlassian Document Format) comment for Jira Cloud.
     */
    private ObjectNode createAdfComment(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "doc");
        body.put("version", 1);

        ArrayNode content = body.putArray("content");

        // Split by lines and create paragraphs
        String[] lines = text.split("\n");
        for (String line : lines) {
            ObjectNode paragraph = objectMapper.createObjectNode();
            paragraph.put("type", "paragraph");

            ArrayNode paragraphContent = paragraph.putArray("content");
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "text");
            textNode.put("text", line);
            paragraphContent.add(textNode);

            content.add(paragraph);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("body", body);
        return result;
    }

    private static String getEnvOrProperty(String envName, String propertyName) {
        return getEnvOrProperty(envName, propertyName, null);
    }

    private static String getEnvOrProperty(String envName, String propertyName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(propertyName);
        }
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}
