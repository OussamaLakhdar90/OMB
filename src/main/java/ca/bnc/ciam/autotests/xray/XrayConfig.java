package ca.bnc.ciam.autotests.xray;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for Xray integration.
 */
@Data
@Builder
public class XrayConfig {

    /**
     * Xray Cloud API base URL.
     */
    @Builder.Default
    private String baseUrl = "https://xray.cloud.getxray.app/api/v2";

    /**
     * Xray client ID for authentication.
     */
    private String clientId;

    /**
     * Xray client secret for authentication.
     */
    private String clientSecret;

    /**
     * Jira project key.
     */
    private String projectKey;

    /**
     * Test plan key (optional).
     */
    private String testPlanKey;

    /**
     * Test execution key (optional, will be created if not provided).
     */
    private String testExecutionKey;

    /**
     * Environment name for reporting.
     */
    @Builder.Default
    private String environment = "default";

    /**
     * Version/Release being tested.
     */
    private String version;

    /**
     * Revision/Build number.
     */
    private String revision;

    /**
     * Whether to create test execution automatically.
     */
    @Builder.Default
    private boolean autoCreateExecution = true;

    /**
     * Whether to update existing test execution.
     */
    @Builder.Default
    private boolean updateExistingExecution = true;

    /**
     * Whether Xray reporting is enabled.
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Connection timeout in milliseconds.
     */
    @Builder.Default
    private int connectionTimeout = 30000;

    /**
     * Read timeout in milliseconds.
     */
    @Builder.Default
    private int readTimeout = 60000;

    /**
     * Create configuration from environment variables.
     */
    public static XrayConfig fromEnvironment() {
        return XrayConfig.builder()
                .clientId(getEnvOrProperty("XRAY_CLIENT_ID", "xray.client.id"))
                .clientSecret(getEnvOrProperty("XRAY_CLIENT_SECRET", "xray.client.secret"))
                .projectKey(getEnvOrProperty("XRAY_PROJECT_KEY", "xray.project.key"))
                .testPlanKey(getEnvOrProperty("XRAY_TEST_PLAN_KEY", "xray.test.plan.key"))
                .testExecutionKey(getEnvOrProperty("XRAY_TEST_EXECUTION_KEY", "xray.test.execution.key"))
                .environment(getEnvOrProperty("XRAY_ENVIRONMENT", "xray.environment", "default"))
                .version(getEnvOrProperty("XRAY_VERSION", "xray.version"))
                .revision(getEnvOrProperty("XRAY_REVISION", "xray.revision"))
                .enabled(Boolean.parseBoolean(getEnvOrProperty("XRAY_ENABLED", "xray.enabled", "true")))
                .build();
    }

    /**
     * Get value from environment variable or system property.
     */
    private static String getEnvOrProperty(String envName, String propertyName) {
        return getEnvOrProperty(envName, propertyName, null);
    }

    /**
     * Get value from environment variable or system property with default.
     */
    private static String getEnvOrProperty(String envName, String propertyName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(propertyName);
        }
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    /**
     * Validate configuration.
     */
    public boolean isValid() {
        return clientId != null && !clientId.isEmpty() &&
                clientSecret != null && !clientSecret.isEmpty() &&
                projectKey != null && !projectKey.isEmpty();
    }
}
