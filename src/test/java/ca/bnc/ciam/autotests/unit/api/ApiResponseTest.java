package ca.bnc.ciam.autotests.unit.api;

import ca.bnc.ciam.autotests.api.ApiResponse;
import ca.bnc.ciam.autotests.api.HttpStatus;
import io.restassured.response.Response;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ApiResponse wrapper.
 */
@Test(groups = "unit")
public class ApiResponseTest {

    private Response mockResponse;

    @BeforeMethod
    public void setUp() {
        mockResponse = Mockito.mock(Response.class);

        // Setup default mock for body (required by ApiResponse constructor)
        io.restassured.response.ResponseBody mockBody = Mockito.mock(io.restassured.response.ResponseBody.class);
        when(mockResponse.getBody()).thenReturn(mockBody);
        when(mockBody.asString()).thenReturn("{}");

        // Setup default mock for time (required by ApiResponse constructor)
        when(mockResponse.getTime()).thenReturn(0L);
    }

    @Test
    public void testStatusCode() {
        when(mockResponse.getStatusCode()).thenReturn(200);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    public void testIsSuccess_2xxCodes() {
        when(mockResponse.getStatusCode()).thenReturn(200);
        assertThat(new ApiResponse(mockResponse).isSuccess()).isTrue();

        when(mockResponse.getStatusCode()).thenReturn(201);
        assertThat(new ApiResponse(mockResponse).isSuccess()).isTrue();

        when(mockResponse.getStatusCode()).thenReturn(204);
        assertThat(new ApiResponse(mockResponse).isSuccess()).isTrue();
    }

    @Test
    public void testIsSuccess_NonSuccessCodes() {
        when(mockResponse.getStatusCode()).thenReturn(400);
        assertThat(new ApiResponse(mockResponse).isSuccess()).isFalse();

        when(mockResponse.getStatusCode()).thenReturn(404);
        assertThat(new ApiResponse(mockResponse).isSuccess()).isFalse();

        when(mockResponse.getStatusCode()).thenReturn(500);
        assertThat(new ApiResponse(mockResponse).isSuccess()).isFalse();
    }

    @Test
    public void testGetBody() {
        String body = "{\"name\":\"test\"}";
        when(mockResponse.getBody()).thenReturn(Mockito.mock(io.restassured.response.ResponseBody.class));
        when(mockResponse.getBody().asString()).thenReturn(body);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getBody()).isEqualTo(body);
    }

    @Test
    public void testGetHeader() {
        when(mockResponse.getHeader("Content-Type")).thenReturn("application/json");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
    }

    @Test
    public void testGetHeaders() {
        io.restassured.http.Headers headers = Mockito.mock(io.restassured.http.Headers.class);
        when(mockResponse.getHeaders()).thenReturn(headers);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getHeaders()).isNotNull();
    }

    @Test
    public void testGetResponseTime() {
        when(mockResponse.getTime()).thenReturn(150L);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getResponseTime()).isEqualTo(150L);
    }

    @Test
    public void testJsonPathString() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getString("name")).thenReturn("testValue");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.jsonPathString("name")).isEqualTo("testValue");
    }

    @Test
    public void testJsonPathInt() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getInt("count")).thenReturn(42);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.jsonPathInt("count")).isEqualTo(42);
    }

    @Test
    public void testGetRawResponse() {
        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getRawResponse()).isEqualTo(mockResponse);
    }

    @Test
    public void testJsonPath_ExtractsValue() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.get("data.id")).thenReturn("12345");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.<String>jsonPath("data.id")).isEqualTo("12345");
    }

    @Test
    public void testJsonPathList() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getList("data.items")).thenReturn(Arrays.asList("item1", "item2", "item3"));

        ApiResponse response = new ApiResponse(mockResponse);
        List<String> items = response.jsonPathList("data.items");

        assertThat(items).containsExactly("item1", "item2", "item3");
    }

    @Test
    public void testIsSuccess() {
        when(mockResponse.getStatusCode()).thenReturn(201);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    public void testIsClientError() {
        when(mockResponse.getStatusCode()).thenReturn(404);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.isClientError()).isTrue();
    }

    @Test
    public void testIsServerError() {
        when(mockResponse.getStatusCode()).thenReturn(503);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.isServerError()).isTrue();
    }

    @Test
    public void testGetStatus() {
        when(mockResponse.getStatusCode()).thenReturn(200);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testAssertStatus_MatchingStatus() {
        when(mockResponse.getStatusCode()).thenReturn(200);

        ApiResponse response = new ApiResponse(mockResponse);

        // Should not throw
        response.assertStatus(200);
    }

    @Test
    public void testAssertSuccess_WithSuccessCode() {
        when(mockResponse.getStatusCode()).thenReturn(200);

        ApiResponse response = new ApiResponse(mockResponse);

        // Should not throw
        response.assertSuccess();
    }

    @Test
    public void testGetCookie() {
        when(mockResponse.getCookie("session")).thenReturn("abc123");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getCookie("session")).isEqualTo("abc123");
    }
}
