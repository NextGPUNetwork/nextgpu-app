package ai.nextgpu.agent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * A utility class to facilitate HTTP operations such as GET and POST requests.
 * Provides methods for sending both unstructured and structured requests, and
 * processing responses, including deserialization to objects.
 *
 * This class uses `HttpURLConnection` for making HTTP requests and
 * `ObjectMapper` for JSON serialization and deserialization.
 */
@SuppressWarnings("deprecation")
@Component
public class HttpUtil {

    private final ObjectMapper objectMapper;
    private final GlobalPropertyRepository globalPropertyRepository;

    @Autowired
    public HttpUtil(GlobalPropertyRepository globalPropertyRepository) {
        this.globalPropertyRepository = globalPropertyRepository;
        this.objectMapper = JsonUtil.OBJECT_MAPPER;
    }

    private String getJwtToken() {
        GlobalProperty property = globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN).orElse(null);
        if (property != null) {
            return "Bearer " + property.getValueReference();
        }
        return null;
    }

    /**
     * Establishes an HTTP connection to the specified URL with the provided configurations.
     * Configures the connection with content type, authorization token, and output settings.
     *
     * @param urlString the URL as a string to which the HTTP connection is made
     * @param doOutput  a boolean indicating whether the connection is used for sending output
     * @param token     the authorization token to be included in the request headers
     * @return an instance of HttpURLConnection configured with the provided parameters
     * @throws Exception if an error occurs during the connection setup
     */
    public HttpURLConnection getHttpConnection(String urlString, boolean doOutput, String token) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty("Accept", MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty("Authorization", token);
        conn.setDoOutput(doOutput);
        return conn;
    }

    /**
     * Sends an HTTP GET request to the specified URL and returns the response as a string.
     *
     * @param urlString the URL to which the GET request is sent
     * @return the response from the server as a string
     * @throws Exception if an error occurs during the connection or while reading the response
     */
    public String get(String urlString, String token) throws Exception {
        HttpURLConnection conn = getHttpConnection(urlString, false, token);
        conn.setRequestMethod("GET");
        return readResponse(conn);
    }

    /**
     * Sends an HTTP GET request to the specified URL and deserializes the response into the specified type.
     *
     * @param <T>          the type of the response object
     * @param urlString    the URL to which the GET request is sent
     * @param responseType the class type to deserialize the response into
     * @param includeAuth  whether to include an authentication token in the request
     * @return the deserialized response object of type T
     * @throws Exception if an error occurs during connection, deserialization, or while reading the response
     */
    public <T> T get(String urlString, Class<T> responseType, boolean includeAuth) throws Exception {
        String token = includeAuth ? getJwtToken() : null;
        String response = get(urlString, token);
        return objectMapper.readValue(response, responseType);
    }

    /**
     * Sends an HTTP POST request to the specified URL with the given body object serialized as JSON
     * and returns the response as a string.
     *
     * @param urlString the URL to which the POST request is sent
     * @param body      the request body object to be serialized to JSON
     * @param token     the authentication token to include in the request
     * @return the response from the server as a string
     * @throws Exception if an error occurs during serialization, connection, or while reading the response
     */
    private String post(String urlString, Object body, String token) throws Exception {
        HttpURLConnection conn = getHttpConnection(urlString, true, token);
        conn.setRequestMethod("POST");

        String jsonInput = objectMapper.writeValueAsString(body);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return readResponse(conn);
    }

    /**
     * Sends an HTTP POST request to the specified URL with the given body object serialized as JSON
     * and deserializes the response into the specified type.
     *
     * @param <T>          the type of the response object
     * @param urlString    the URL to which the POST request is sent
     * @param body         the request body object to be serialized to JSON
     * @param responseType the class type to deserialize the response into
     * @param includeAuth  whether to include an authentication token in the request
     * @return the deserialized response object of type T
     * @throws Exception if an error occurs during serialization, connection, deserialization, or while reading the response
     */
    public <T> T post(String urlString, Object body, Class<T> responseType, boolean includeAuth) throws Exception {
        String token = includeAuth ? getJwtToken() : null;
        String response = post(urlString, body, token);
        return objectMapper.readValue(response, responseType);
    }


    /**
     * Sends an HTTP DELETE request to the specified URL.
     *
     * @param urlString the URL to which the DELETE request is sent
     * @return the response from the server as a string (empty for 204 No Content)
     * @throws Exception if an error occurs during the connection or while reading the response
     */
    public String delete(String urlString) throws Exception {
        HttpURLConnection conn = getHttpConnection(urlString, false, getJwtToken());
        conn.setRequestMethod("DELETE");

        return readResponse(conn);
    }

    /**
     * Sends an HTTP DELETE request to the specified URL with the provided headers.
     *
     * @param urlString the URL to which the DELETE request is sent
     * @param headers a map of header keys and values to include in the request
     * @return the response from the server as a string (empty for 204 No Content)
     * @throws Exception if an error occurs during the connection or while reading the response
     */
    public String delete(String urlString, Map<String, String> headers) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Accept", MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty("Authorization", getJwtToken());

        for (Map.Entry<String, String> header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        return readResponse(conn);
    }

    /**
     * Calls REST API to return a structured response generated by LLM.
     *
     * @param context not required if shortResponse is true
     * @param prompt the input prompt to pass to LLM
     * @param schemaMap a map of variable names and data types to help LLM understand the desired output
     * @return Map of generated structured response
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStructuredAiResponse(String hostBaseUrl, String context, String prompt, Map<String, String> schemaMap) throws Exception {
        String url = hostBaseUrl + "/api/ai/structured";
        Map<String, Object> request = new HashMap<>();
        request.put("context", context);
        request.put("prompt", prompt);
        request.put("schema", schemaMap);
        String response = post(url, objectMapper.writeValueAsString(request), String.class, true);
        return objectMapper.readValue(response, Map.class);
    }

    /* *************** */
    /* * AI Services * */
    /* *************** */
    /**
     * Calls REST API to return the response generated by LLM (Large Language Model).
     * The method supports both short and standard responses based on the shortResponse flag.
     *
     * @param hostBaseUrl the base URL of the host server (e.g., from nextgpu.web.baseUrl configuration)
     * @param context the context information to provide to the LLM; not required if shortResponse is true
     * @param prompt the input prompt to pass to the LLM
     * @param shortResponse when true, returns a short response under 100 tokens; when false, returns an unstructured response
     * @return the generated response from the LLM as a string
     * @throws Exception if an error occurs during the HTTP request or response processing
     */
    public String getAiResponse(String hostBaseUrl, String context, String prompt, boolean shortResponse) throws Exception {
        String url = hostBaseUrl + "/api/ai/unstructured";
        Map<String, String> request = new HashMap<>();
        request.put("prompt", prompt);
        if (shortResponse) {
            url = hostBaseUrl + "/api/ai/short";
            request.put("context", context);
        }
        return post(url, objectMapper.writeValueAsString(request), String.class, true);
    }

    /**
     * Reads the response from an HTTP connection and returns it as a string.
     * This helper method processes the input stream from the connection and
     * accumulates the response lines into a single string.
     *
     * @param conn the HttpURLConnection from which to read the response
     * @return the complete response as a string with trimmed lines
     * @throws Exception if an error occurs while reading the input stream
     */
    private String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();

        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return "";
        }

        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

}
