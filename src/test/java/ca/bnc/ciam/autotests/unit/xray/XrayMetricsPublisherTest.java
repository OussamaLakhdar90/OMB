package ca.bnc.ciam.autotests.unit.xray;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import ca.bnc.ciam.autotests.xray.JiraClient;
import ca.bnc.ciam.autotests.xray.XrayMetricsPublisher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XrayMetricsPublisher.
 * Tests publishing of TestMetrics to Jira Test Plans as comments.
 */
@Test(groups = "unit")
public class XrayMetricsPublisherTest {

    @Mock
    private JiraClient mockJiraClient;

    private XrayMetricsPublisher publisher;
    private TestMetrics metrics;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        publisher = XrayMetricsPublisher.builder()
                .jiraClient(mockJiraClient)
                .compactMode(false)
                .build();

        // Create sample metrics
        metrics = TestMetrics.builder()
                .runId("test-run-123")
                .suiteName("Login Tests")
                .startTime(LocalDateTime.of(2026, 1, 21, 14, 30, 0))
                .endTime(LocalDateTime.of(2026, 1, 21, 14, 35, 32))
                .durationMs(332000)
                .environment("staging-ta")
                .browser("Chrome 120")
                .executionMode("saucelabs")
                .totalTests(50)
                .passedTests(45)
                .failedTests(3)
                .skippedTests(2)
                .passRate(90.0)
                .testResults(new ArrayList<>())
                .apiMetrics(new ArrayList<>())
                .visualMetrics(new ArrayList<>())
                .customMetrics(new HashMap<>())
                .build();
    }

    // ===========================================
    // publishToTestPlan() Tests
    // ===========================================

    @Test
    public void testPublishToTestPlan_ValidInputs_CallsJiraClient() {
        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString())).thenReturn(true);

        boolean result = publisher.publishToTestPlan("PROJ-123", metrics);

        assertThat(result).isTrue();
        verify(mockJiraClient).addWikiMarkupComment(eq("PROJ-123"), anyString());
    }

    @Test
    public void testPublishToTestPlan_JiraClientNotValid_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(false);

        boolean result = publisher.publishToTestPlan("PROJ-123", metrics);

        assertThat(result).isFalse();
        verify(mockJiraClient, never()).addWikiMarkupComment(anyString(), anyString());
    }

    @Test
    public void testPublishToTestPlan_NullTestPlanKey_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);

        boolean result = publisher.publishToTestPlan(null, metrics);

        assertThat(result).isFalse();
        verify(mockJiraClient, never()).addWikiMarkupComment(anyString(), anyString());
    }

    @Test
    public void testPublishToTestPlan_EmptyTestPlanKey_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);

        boolean result = publisher.publishToTestPlan("", metrics);

        assertThat(result).isFalse();
        verify(mockJiraClient, never()).addWikiMarkupComment(anyString(), anyString());
    }

    @Test
    public void testPublishToTestPlan_NullMetrics_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);

        boolean result = publisher.publishToTestPlan("PROJ-123", null);

        assertThat(result).isFalse();
        verify(mockJiraClient, never()).addWikiMarkupComment(anyString(), anyString());
    }

    @Test
    public void testPublishToTestPlan_JiraClientFails_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString())).thenReturn(false);

        boolean result = publisher.publishToTestPlan("PROJ-123", metrics);

        assertThat(result).isFalse();
    }

    @Test
    public void testPublishToTestPlan_CommentContainsMetrics() {
        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString())).thenReturn(true);

        publisher.publishToTestPlan("PROJ-123", metrics);

        verify(mockJiraClient).addWikiMarkupComment(eq("PROJ-123"), argThat(comment ->
                comment.contains("test-run-123") &&
                comment.contains("Login Tests") &&
                comment.contains("staging-ta") &&
                comment.contains("90.0%")
        ));
    }

    // ===========================================
    // publishToTestExecution() Tests
    // ===========================================

    @Test
    public void testPublishToTestExecution_ValidInputs_CallsJiraClient() {
        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString())).thenReturn(true);

        boolean result = publisher.publishToTestExecution("PROJ-456", metrics);

        assertThat(result).isTrue();
        verify(mockJiraClient).addWikiMarkupComment(eq("PROJ-456"), anyString());
    }

    @Test
    public void testPublishToTestExecution_NullKey_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);

        boolean result = publisher.publishToTestExecution(null, metrics);

        assertThat(result).isFalse();
        verify(mockJiraClient, never()).addWikiMarkupComment(anyString(), anyString());
    }

    @Test
    public void testPublishToTestExecution_EmptyKey_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);

        boolean result = publisher.publishToTestExecution("", metrics);

        assertThat(result).isFalse();
        verify(mockJiraClient, never()).addWikiMarkupComment(anyString(), anyString());
    }

    // ===========================================
    // Compact Mode Tests
    // ===========================================

    @Test
    public void testPublishToTestPlan_CompactMode_UsesCompactFormat() {
        XrayMetricsPublisher compactPublisher = XrayMetricsPublisher.builder()
                .jiraClient(mockJiraClient)
                .compactMode(true)
                .build();

        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString())).thenReturn(true);

        compactPublisher.publishToTestPlan("PROJ-123", metrics);

        // Compact format should not contain "h2. Test Execution Metrics" but "h3. Test Run:"
        verify(mockJiraClient).addWikiMarkupComment(eq("PROJ-123"), argThat(comment ->
                comment.contains("h3. Test Run:") &&
                !comment.contains("h2. Test Execution Metrics")
        ));
    }

    @Test
    public void testPublishToTestPlan_FullMode_UsesFullFormat() {
        XrayMetricsPublisher fullPublisher = XrayMetricsPublisher.builder()
                .jiraClient(mockJiraClient)
                .compactMode(false)
                .build();

        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString())).thenReturn(true);

        fullPublisher.publishToTestPlan("PROJ-123", metrics);

        verify(mockJiraClient).addWikiMarkupComment(eq("PROJ-123"), argThat(comment ->
                comment.contains("h2. Test Execution Metrics") &&
                comment.contains("h3. Execution Summary") &&
                comment.contains("h3. Test Results")
        ));
    }

    // ===========================================
    // isConfigured() Tests
    // ===========================================

    @Test
    public void testIsConfigured_ValidJiraClient_ReturnsTrue() {
        when(mockJiraClient.isValid()).thenReturn(true);

        assertThat(publisher.isConfigured()).isTrue();
    }

    @Test
    public void testIsConfigured_InvalidJiraClient_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(false);

        assertThat(publisher.isConfigured()).isFalse();
    }

    @Test
    public void testIsConfigured_NullJiraClient_ReturnsFalse() {
        XrayMetricsPublisher nullClientPublisher = XrayMetricsPublisher.builder()
                .jiraClient(null)
                .compactMode(false)
                .build();

        assertThat(nullClientPublisher.isConfigured()).isFalse();
    }

    // ===========================================
    // Exception Handling Tests
    // ===========================================

    @Test
    public void testPublishToTestPlan_JiraClientThrowsException_ReturnsFalse() {
        when(mockJiraClient.isValid()).thenReturn(true);
        when(mockJiraClient.addWikiMarkupComment(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection failed"));

        boolean result = publisher.publishToTestPlan("PROJ-123", metrics);

        assertThat(result).isFalse();
    }
}
