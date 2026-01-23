package ca.bnc.ciam.autotests.xray;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Publishes test metrics to Xray/Jira Test Plans as comments.
 *
 * After test execution completes, this publisher formats the TestMetrics
 * as Jira Wiki Markup and adds them as a comment to the Test Plan issue.
 *
 * Usage:
 * <pre>
 * TestMetrics metrics = MetricsCollector.getInstance().getCurrentMetrics();
 *
 * XrayMetricsPublisher publisher = XrayMetricsPublisher.fromEnvironment();
 * publisher.publishToTestPlan("PROJ-123", metrics);
 * </pre>
 *
 * Environment Variables:
 * - JIRA_BASE_URL: Jira instance URL (e.g., https://company.atlassian.net)
 * - JIRA_USERNAME: Jira username (email for Cloud)
 * - JIRA_API_TOKEN: API token
 * - JIRA_IS_CLOUD: true for Cloud, false for Server (default: true)
 * - XRAY_TEST_PLAN_KEY: Test Plan issue key (e.g., PROJ-100)
 */
@Slf4j
public class XrayMetricsPublisher {

    private final JiraClient jiraClient;
    private final boolean compactMode;

    /**
     * Create publisher with Jira client.
     *
     * @param jiraClient  Jira client for API calls
     * @param compactMode true to use compact format, false for full metrics
     */
    @Builder
    public XrayMetricsPublisher(JiraClient jiraClient, boolean compactMode) {
        this.jiraClient = jiraClient;
        this.compactMode = compactMode;
    }

    /**
     * Create publisher from environment variables.
     */
    public static XrayMetricsPublisher fromEnvironment() {
        return XrayMetricsPublisher.builder()
                .jiraClient(JiraClient.fromEnvironment())
                .compactMode(Boolean.parseBoolean(
                        System.getProperty("xray.metrics.compact", "false")))
                .build();
    }

    /**
     * Publish metrics to a Test Plan as a comment.
     *
     * @param testPlanKey Test Plan issue key (e.g., PROJ-100)
     * @param metrics     Test metrics to publish
     * @return true if successful
     */
    public boolean publishToTestPlan(String testPlanKey, TestMetrics metrics) {
        if (testPlanKey == null || testPlanKey.isEmpty()) {
            log.warn("Test Plan key is not configured. Skipping metrics publishing.");
            return false;
        }

        if (metrics == null) {
            log.warn("Metrics are null. Skipping metrics publishing.");
            return false;
        }

        if (!jiraClient.isValid()) {
            log.warn("Jira client is not configured. Skipping metrics publishing.");
            return false;
        }

        try {
            String formattedMetrics = formatMetrics(metrics);
            boolean success = jiraClient.addWikiMarkupComment(testPlanKey, formattedMetrics);

            if (success) {
                log.info("Successfully published metrics to Test Plan {}", testPlanKey);
            } else {
                log.error("Failed to publish metrics to Test Plan {}", testPlanKey);
            }

            return success;
        } catch (Exception e) {
            log.error("Error publishing metrics to Test Plan {}", testPlanKey, e);
            return false;
        }
    }

    /**
     * Publish metrics to Test Plan configured in environment.
     *
     * @param metrics Test metrics to publish
     * @return true if successful
     */
    public boolean publishToTestPlan(TestMetrics metrics) {
        String testPlanKey = getTestPlanKeyFromEnvironment();
        return publishToTestPlan(testPlanKey, metrics);
    }

    /**
     * Publish metrics to Test Execution as a comment.
     *
     * @param testExecutionKey Test Execution issue key
     * @param metrics          Test metrics to publish
     * @return true if successful
     */
    public boolean publishToTestExecution(String testExecutionKey, TestMetrics metrics) {
        if (testExecutionKey == null || testExecutionKey.isEmpty()) {
            log.warn("Test Execution key is not provided. Skipping metrics publishing.");
            return false;
        }

        if (metrics == null) {
            log.warn("Metrics are null. Skipping metrics publishing.");
            return false;
        }

        if (!jiraClient.isValid()) {
            log.warn("Jira client is not configured. Skipping metrics publishing.");
            return false;
        }

        try {
            String formattedMetrics = formatMetrics(metrics);
            boolean success = jiraClient.addWikiMarkupComment(testExecutionKey, formattedMetrics);

            if (success) {
                log.info("Successfully published metrics to Test Execution {}", testExecutionKey);
            } else {
                log.error("Failed to publish metrics to Test Execution {}", testExecutionKey);
            }

            return success;
        } catch (Exception e) {
            log.error("Error publishing metrics to Test Execution {}", testExecutionKey, e);
            return false;
        }
    }

    /**
     * Format metrics based on mode (compact or full).
     */
    private String formatMetrics(TestMetrics metrics) {
        if (compactMode) {
            return JiraWikiMarkupFormatter.formatCompact(metrics);
        } else {
            return JiraWikiMarkupFormatter.format(metrics);
        }
    }

    /**
     * Get Test Plan key from environment.
     */
    private String getTestPlanKeyFromEnvironment() {
        String key = System.getenv("XRAY_TEST_PLAN_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getProperty("xray.test.plan.key");
        }
        return key;
    }

    /**
     * Check if publisher is properly configured.
     */
    public boolean isConfigured() {
        return jiraClient != null && jiraClient.isValid();
    }
}
