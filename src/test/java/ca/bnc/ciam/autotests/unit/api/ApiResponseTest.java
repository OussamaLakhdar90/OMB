package ca.bnc.ciam.autotests.unit.api;

import ca.bnc.ciam.autotests.api.ApiResponse;
import io.restassured.response.Response;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    @Test
    public void testStatusCode() {
        when(mockResponse.getStatusCode()).thenReturn(200);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    public void testIsSuccessful_2xxCodes() {
        when(mockResponse.getStatusCode()).thenReturn(200);
        assertThat(new ApiResponse(mockResponse).isSuccessful()).isTrue();

        when(mockResponse.getStatusCode()).thenReturn(201);
        assertThat(new ApiResponse(mockResponse).isSuccessful()).isTrue();

        when(mockResponse.getStatusCode()).thenReturn(204);
        assertThat(new ApiResponse(mockResponse).isSuccessful()).isTrue();
    }

    @Test
    public void testIsSuccessful_NonSuccessCodes() {
        when(mockResponse.getStatusCode()).thenReturn(400);
        assertThat(new ApiResponse(mockResponse).isSuccessful()).isFalse();

        when(mockResponse.getStatusCode()).thenReturn(404);
        assertThat(new ApiResponse(mockResponse).isSuccessful()).isFalse();

        when(mockResponse.getStatusCode()).thenReturn(500);
        assertThat(new ApiResponse(mockResponse).isSuccessful()).isFalse();
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
    public void testGetTime() {
        when(mockResponse.getTime()).thenReturn(150L);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getTime()).isEqualTo(150L);
    }

    @Test
    public void testJsonPath() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getString("name")).thenReturn("testValue");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.jsonPath().getString("name")).isEqualTo("testValue");
    }

    @Test
    public void testStatusLine() {
        when(mockResponse.getStatusLine()).thenReturn("HTTP/1.1 200 OK");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getStatusLine()).isEqualTo("HTTP/1.1 200 OK");
    }

    @Test
    public void testContentType() {
        when(mockResponse.getContentType()).thenReturn("application/json; charset=utf-8");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getContentType()).isEqualTo("application/json; charset=utf-8");
    }

    @Test
    public void testGetRawResponse() {
        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.getRawResponse()).isEqualTo(mockResponse);
    }

    @Test
    public void testExtract_JsonPath() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getString("data.id")).thenReturn("12345");

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.extract("data.id")).isEqualTo("12345");
    }

    @Test
    public void testExtractList() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getList("data.items")).thenReturn(Arrays.asList("item1", "item2", "item3"));

        ApiResponse response = new ApiResponse(mockResponse);
        List<String> items = response.extractList("data.items");

        assertThat(items).containsExactly("item1", "item2", "item3");
    }

    @Test
    public void testExtractMap() {
        io.restassured.path.json.JsonPath jsonPath = Mockito.mock(io.restassured.path.json.JsonPath.class);
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        when(mockResponse.jsonPath()).thenReturn(jsonPath);
        when(jsonPath.getMap("data")).thenReturn(data);

        ApiResponse response = new ApiResponse(mockResponse);
        Map<String, Object> extracted = response.extractMap("data");

        assertThat(extracted).containsEntry("key1", "value1");
        assertThat(extracted).containsEntry("key2", "value2");
    }

    @Test
    public void testIs2xx() {
        when(mockResponse.getStatusCode()).thenReturn(201);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.is2xx()).isTrue();
    }

    @Test
    public void testIs4xx() {
        when(mockResponse.getStatusCode()).thenReturn(404);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.is4xx()).isTrue();
    }

    @Test
    public void testIs5xx() {
        when(mockResponse.getStatusCode()).thenReturn(503);

        ApiResponse response = new ApiResponse(mockResponse);

        assertThat(response.is5xx()).isTrue();
    }
}
