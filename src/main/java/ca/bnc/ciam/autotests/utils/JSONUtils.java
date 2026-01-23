package ca.bnc.ciam.autotests.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Utility class for JSON operations.
 */
@Slf4j
public final class JSONUtils {

    private static final ObjectMapper objectMapper = createObjectMapper();

    private JSONUtils() {
        // Utility class - prevent instantiation
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Get the shared ObjectMapper instance.
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Read a JSON file and return as JSONObject.
     */
    public static JSONObject readJSONFromFile(File file, Charset charset) throws IOException, JSONException {
        String content = Files.readString(file.toPath(), charset);
        return new JSONObject(content);
    }

    /**
     * Read a JSON file and return as JSONObject using UTF-8.
     */
    public static JSONObject readJSONFromFile(File file) throws IOException, JSONException {
        return readJSONFromFile(file, StandardCharsets.UTF_8);
    }

    /**
     * Read a JSON file and return as JSONArray.
     */
    public static JSONArray readJSONArrayFromFile(File file, Charset charset) throws IOException, JSONException {
        String content = Files.readString(file.toPath(), charset);
        return new JSONArray(content);
    }

    /**
     * Read a JSON file and deserialize to a specific type.
     */
    public static <T> T readFromFile(File file, Class<T> clazz) throws IOException {
        return objectMapper.readValue(file, clazz);
    }

    /**
     * Read a JSON file and deserialize to a specific type using TypeReference.
     */
    public static <T> T readFromFile(File file, TypeReference<T> typeReference) throws IOException {
        return objectMapper.readValue(file, typeReference);
    }

    /**
     * Write an object to a JSON file.
     */
    public static void writeToFile(File file, Object object) throws IOException {
        objectMapper.writeValue(file, object);
    }

    /**
     * Convert an object to JSON string.
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return "{}";
        }
    }

    /**
     * Convert an object to pretty-printed JSON string.
     */
    public static String toPrettyJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return "{}";
        }
    }

    /**
     * Parse a JSON string to a specific type.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON string", e);
            return null;
        }
    }

    /**
     * Parse a JSON string to a Map.
     */
    public static Map<String, Object> jsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON string to map", e);
            return null;
        }
    }

    /**
     * Convert a Map to JSONObject.
     */
    public static JSONObject mapToJSONObject(Map<String, ?> map) {
        return new JSONObject(map);
    }

    /**
     * Check if a string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (JSONException e) {
            try {
                new JSONArray(json);
                return true;
            } catch (JSONException ex) {
                return false;
            }
        }
    }
}
