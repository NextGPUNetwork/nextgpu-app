package ai.nextgpu.agent.util;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.exception.ApiException;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.common.dto.ErrorResponseDto;
import ai.nextgpu.common.dto.StructuredAiRequestDto;
import ai.nextgpu.common.exception.ErrorCode;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.common.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
@Component
public class HttpUtil {

    private record HttpResponse(int status, String body) {
    }

    private final ObjectMapper objectMapper;
    private final GlobalPropertyRepository globalPropertyRepository;

    @Autowired
    public HttpUtil(GlobalPropertyRepository globalPropertyRepository) {
        this.globalPropertyRepository = globalPropertyRepository;
        this.objectMapper = JsonUtil.OBJECT_MAPPER;
    }

    /**
     * Retrieves the JWT authorization token from the database.
     * <p>
     * This method looks up the {@link GlobalPropertyConfig#JWT_TOKEN} property.
     * If present, it formats it with the "Bearer " prefix; otherwise, it returns {@code null}.
     * </p>
     *
     * @return the formatted "Bearer <token>" string, or {@code null} if the token is not configured
     */
    private String getJwtToken() {
        GlobalProperty property = globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN).orElse(null);
        if (property == null) {
            return null;
        }
        return "Bearer " + property.getValueReference();
    }

    /**
     * Creates and configures a default {@link HttpURLConnection} for JSON-based requests.
     * <p>
     * This helper configures the HTTP request to send and accept JSON content.
     * It also conditionally appends the "Authorization" header if a token is provided.
     * </p>
     *
     * @param urlString the destination URL endpoint
     * @param doOutput  {@code true} if the request intends to write a body payload (such as POST);
     *                  {@code false} otherwise
     * @param token     the authorization token to include in the header; can be {@code null} or empty
     * @return the configured {@link HttpURLConnection} instance
     * @throws Exception if the URL is malformed or the connection fails to open
     */
    private HttpURLConnection getHttpConnection(String urlString, boolean doOutput, String token) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(doOutput);
        conn.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        conn.setRequestProperty("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", token);
        }
        return conn;
    }

    /**
     * Constructs a unified {@link ApiException} from an unsuccessful HTTP response.
     * <p>
     * This method attempts to deserialize the server's error payload into an
     * {@link ErrorResponseDto}. If deserialization fails (e.g., because the server returned
     * non-JSON text or a different schema), it falls back to building a generic error DTO
     * with an {@link ErrorCode#UNKNOWN_ERROR} code containing the raw response body.
     * </p>
     *
     * @param response the failed {@link HttpResponse} containing the status code and raw error body
     * @param url      the destination URL that triggered the error, used for error tracking
     * @return an {@link ApiException} wrapping the parsed or fallback error details
     */
    private ApiException buildApiException(HttpResponse response, String url) {
        ErrorResponseDto error;
        try {
            error = objectMapper.readValue(response.body(), ErrorResponseDto.class);
        } catch (Exception ex) {
            error = new ErrorResponseDto(Instant.now(), ErrorCode.UNKNOWN_ERROR, response.body(), url);
        }
        return new ApiException(error);
    }

    /**
     * Parses a successful HTTP response into the specified Jackson {@link JavaType},
     * or throws a unified exception if the response indicates an error.
     * <p>
     * If the response status is within the {@code 2xx} range, this method deserializes the body
     * using the provided {@link JavaType}. If the status is {@code 204 No Content} or the body
     * is blank, it safely returns {@code null}. For all non-{@code 2xx} statuses, it constructs
     * and throws an {@link ApiException}.
     * </p>
     *
     * @param <T>      the expected target type of the deserialized response
     * @param response the {@link HttpResponse} received from the connection
     * @param url      the destination URL that was requested, used for error tracing
     * @param javaType the Jackson-resolved target type information
     * @return the deserialized response object of type {@code T}, or {@code null} if the response is empty
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an error occurs during JSON deserialization
     */
    private <T> T parseResponse(HttpResponse response, String url, JavaType javaType) throws Exception {
        if (response.status() >= 200 && response.status() < 300) {
            if (response.status() == HttpURLConnection.HTTP_NO_CONTENT || response.body().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.body(), javaType);
        }

        throw buildApiException(response, url);
    }

    /**
     * Executes a private HTTP GET request to the specified URL.
     *
     * @param urlString the target URL for the GET request
     * @param token     the authorization token (e.g., Bearer token) to include in the headers;
     *                  can be {@code null} or blank if no authorization is required
     * @return the {@link HttpResponse} containing the status code and response body
     * @throws Exception if an error occurs while opening the connection, setting the request method,
     *                   or reading the response
     */
    private HttpResponse get(String urlString, String token) throws Exception {
        HttpURLConnection conn = getHttpConnection(urlString, false, token);
        conn.setRequestMethod("GET");
        return readResponse(conn);
    }

    /**
     * Performs an HTTP GET request and deserializes the JSON response into a concrete Java class.
     * <p>
     * This method automatically resolves the requested class to a {@link JavaType} internally
     * to perform the deserialization using the shared {@code parseResponse} logic.
     * </p>
     *
     * @param <T>          the expected target class type
     * @param urlString    the target URL for the GET request
     * @param responseType the {@link Class} representing the target type
     * @param includeAuth  {@code true} to retrieve and append the JWT authorization token;
     *                     {@code false} to skip authorization
     * @return the deserialized response object of type {@code T}, or {@code null} if the response is empty
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O, network, or deserialization error occurs
     */
    public <T> T get(String urlString, Class<T> responseType, boolean includeAuth) throws Exception {
        String token = includeAuth ? getJwtToken() : null;
        HttpResponse response = get(urlString, token);
        return parseResponse(response, urlString, objectMapper.getTypeFactory().constructType(responseType));
    }

    /**
     * Performs an HTTP GET request and deserializes the JSON response into a complex, generic,
     * or parameterized Java type (e.g., {@code List<MyDto>}).
     * <p>
     * This method automatically resolves the requested {@link TypeReference} to a {@link JavaType}
     * internally to support advanced deserialization while leveraging the shared {@code parseResponse} logic.
     * </p>
     *
     * @param <T>          the expected target type
     * @param urlString    the target URL for the GET request
     * @param responseType the {@link TypeReference} preserving generic type parameters
     * @param includeAuth  {@code true} to retrieve and append the JWT authorization token;
     *                     {@code false} to skip authorization
     * @return the deserialized response object of type {@code T}, or {@code null} if the response is empty
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O, network, or deserialization error occurs
     */
    public <T> T get(String urlString, TypeReference<T> responseType, boolean includeAuth) throws Exception {
        String token = includeAuth ? getJwtToken() : null;
        HttpResponse response = get(urlString, token);
        return parseResponse(response, urlString, objectMapper.getTypeFactory().constructType(responseType));
    }

    /**
     * Executes a private HTTP POST request with a JSON payload to the specified URL.
     * <p>
     * This method serializes the provided Java object body into a JSON string, writes it
     * to the connection's output stream, and returns the server's response.
     * </p>
     *
     * @param urlString the destination URL for the POST request
     * @param body      the payload object to be serialized into the request body
     * @param token     the authorization token (e.g., Bearer token) to include in the headers;
     *                  can be {@code null} or blank if no authorization is required
     * @return the {@link HttpResponse} containing the status code and response body
     * @throws Exception if an error occurs during JSON serialization, opening the connection,
     *                   writing the request payload, or reading the response
     */
    private HttpResponse post(String urlString, Object body, String token) throws Exception {
        HttpURLConnection conn = getHttpConnection(urlString, true, token);
        conn.setRequestMethod("POST");
        String json = objectMapper.writeValueAsString(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    /**
     * Performs an HTTP POST request with a JSON payload and deserializes the JSON response
     * into a concrete Java class.
     * <p>
     * This method resolves the target class to a {@link JavaType} internally to handle
     * deserialization via the shared {@code parseResponse} logic.
     * </p>
     *
     * @param <T>          the expected target class type of the response payload
     * @param urlString    the destination URL for the POST request
     * @param body         the payload object to be serialized into the request body
     * @param responseType the {@link Class} representing the target type
     * @param includeAuth  {@code true} to retrieve and append the JWT authorization token;
     *                     {@code false} to skip authorization
     * @return the deserialized response object of type {@code T}, or {@code null} if the response is empty
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O, serialization, network, or deserialization error occurs
     */
    public <T> T post(String urlString, Object body, Class<T> responseType, boolean includeAuth) throws Exception {
        String token = includeAuth ? getJwtToken() : null;
        HttpResponse response = post(urlString, body, token);
        return parseResponse(response, urlString, objectMapper.getTypeFactory().constructType(responseType));
    }

    /**
     * Executes a private HTTP POST request with {@code multipart/form-data} encoding to the specified URL.
     * <p>
     * This method builds a multipart body conforming to RFC 7578. It iterates over the provided
     * parameter map and appends fields using a unique boundary separator:
     * <ul>
     *   <li>{@link java.io.File File} parameters are written as binary streams, attempting to detect
     *   their content-type or defaulting to {@code application/octet-stream}.</li>
     *   <li>Other objects are serialized to string values and written with a {@code text/plain} content-type.</li>
     * </ul>
     * </p>
     *
     * @param urlString     the destination URL for the multipart POST request
     * @param multipartBody a map containing key-value pairs representing the form fields and files
     * @param token         the authorization token (e.g., Bearer token) to include in the headers;
     *                      can be {@code null} or blank if no authorization is required
     * @return the {@link HttpResponse} containing the status code and response body
     * @throws Exception if an error occurs while probing file content types, opening the connection,
     *                   writing data to the output stream, or reading the server response
     */
    private HttpResponse postMultipart(String urlString, Map<String, Object> multipartBody, String token) throws Exception {
        String boundary = "----NextGpuBoundary" + UUID.randomUUID();
        String lineBreak = "\r\n";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", token);
        }
        try (OutputStream os = conn.getOutputStream()) {
            for (Map.Entry<String, Object> entry : multipartBody.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                os.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));

                if (value instanceof File file) {
                    String contentType = Files.probeContentType(file.toPath());
                    if (contentType == null || contentType.isBlank()) {
                        contentType = "application/octet-stream";
                    }
                    os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
                    os.write(("Content-Type: " + contentType + lineBreak).getBytes(StandardCharsets.UTF_8));
                    os.write(lineBreak.getBytes(StandardCharsets.UTF_8));
                    Files.copy(file.toPath(), os);
                    os.write(lineBreak.getBytes(StandardCharsets.UTF_8));
                } else {
                    os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
                    os.write(("Content-Type: text/plain; charset=UTF-8" + lineBreak).getBytes(StandardCharsets.UTF_8));
                    os.write(lineBreak.getBytes(StandardCharsets.UTF_8));
                    os.write(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
                    os.write(lineBreak.getBytes(StandardCharsets.UTF_8));
                }
            }
            os.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    /**
     * Performs an HTTP POST request with {@code multipart/form-data} encoding and deserializes
     * the response into a concrete Java class.
     * <p>
     * This method automatically handles resolving the destination class to a {@link JavaType}
     * internally to support standardized response validation and deserialization.
     * </p>
     *
     * @param <T>          the expected target class type of the response payload
     * @param urlString    the destination URL for the multipart POST request
     * @param body         a map containing the form fields and files to submit
     * @param responseType the {@link Class} representing the target type
     * @param includeAuth  {@code true} to retrieve and append the JWT authorization token;
     *                     {@code false} to skip authorization
     * @return the deserialized response object of type {@code T}, or {@code null} if the response is empty
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O, serialization, network, or deserialization error occurs
     */
    public <T> T postMultipart(String urlString, Map<String, Object> body, Class<T> responseType, boolean includeAuth) throws Exception {
        String token = includeAuth ? getJwtToken() : null;
        HttpResponse response = postMultipart(urlString, body, token);
        return parseResponse(response, urlString, objectMapper.getTypeFactory().constructType(responseType));
    }

    /**
     * Sends a private HTTP DELETE request to the specified URL.
     *
     * @param urlString the target URL for the DELETE request
     * @param headers   a map of HTTP request headers to be set on the connection; can be {@code null}
     * @param token     the authorization token (e.g., Bearer token) to include in the headers;
     *                  can be {@code null} or blank if no authorization is required
     * @return the {@link HttpResponse} containing the status code and response body
     * @throws Exception if an error occurs while opening the connection, setting request properties,
     *                   or reading the server response
     */
    private HttpResponse delete(String urlString, Map<String, String> headers, String token) throws Exception {
        HttpURLConnection conn = getHttpConnection(urlString, false, token);
        conn.setRequestMethod("DELETE");
        if (headers != null) {
            headers.forEach(conn::setRequestProperty);
        }
        return readResponse(conn);
    }

    /**
     * Performs an authorized HTTP DELETE request on the specified URL.
     * <p>
     * This method automatically retrieves the JWT token for authorization and expects
     * a successful empty or text/plain response.
     * </p>
     *
     * @param urlString the target URL for the DELETE request
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O or network error occurs during execution
     */
    public void delete(String urlString) throws Exception {
        HttpResponse response = delete(urlString, null, getJwtToken());
        parseResponse(response, urlString, objectMapper.getTypeFactory().constructType(String.class));
    }

    /**
     * Performs an authorized HTTP DELETE request on the specified URL with custom headers.
     * <p>
     * This method automatically retrieves the JWT token for authorization, appends the provided
     * custom headers, and expects a successful empty or text/plain response.
     * </p>
     *
     * @param urlString the target URL for the DELETE request
     * @param headers   a map of additional HTTP headers to include in the request
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O or network error occurs during execution
     */
    public void delete(String urlString, Map<String, String> headers) throws Exception {
        HttpResponse response = delete(urlString, headers, getJwtToken());
        parseResponse(response, urlString, objectMapper.getTypeFactory().constructType(String.class));
    }


    /**
     * Sends a request to the structured AI endpoint to retrieve a schema-conforming response.
     * <p>
     * This method packages the context, prompt, and target schema mapping into a
     * {@code StructuredAiRequestDto} and posts it to the {@code /api/ai/structured} endpoint.
     * </p>
     *
     * @param hostBaseUrl the base URL of the target API host
     * @param context     the background context or system instructions for the AI model
     * @param prompt      the user prompt containing the core request or question
     * @param schemaMap   a map defining the target JSON schema structure the response must conform to
     * @return a {@link Map} representing the structured JSON response returned by the AI
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O, serialization, or network error occurs
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStructuredAiResponse(String hostBaseUrl, String context, String prompt, Map<String, String> schemaMap) throws Exception {

        String url = hostBaseUrl + "/api/ai/structured";

        StructuredAiRequestDto request = new StructuredAiRequestDto(context, prompt, schemaMap);

        return post(url, request, Map.class, true);
    }

    /**
     * Sends a request to retrieve a plain-text/unstructured AI response.
     * <p>
     * Depending on the {@code shortResponse} flag, this method routes to one of two endpoints:
     * <ul>
     *   <li>{@code /api/ai/short} (when {@code shortResponse} is {@code true}): A concise, prompt-only completion request.</li>
     *   <li>{@code /api/ai/unstructured} (when {@code shortResponse} is {@code false}): A standard completion request including full context.</li>
     * </ul>
     * </p>
     *
     * @param hostBaseUrl   the base URL of the target API host
     * @param context       the background context or system instructions (ignored if {@code shortResponse} is {@code true})
     * @param prompt        the user prompt containing the core request or question
     * @param shortResponse {@code true} to request a brief response omitting context; {@code false} to include context
     * @return the plain-text AI response as a {@link String}
     * @throws ApiException if the server returns a non-2xx status code
     * @throws Exception    if an I/O, serialization, or network error occurs
     */
    public String getAiResponse(String hostBaseUrl, String context, String prompt, boolean shortResponse) throws Exception {

        String url;

        Map<String, String> request = new HashMap<>();

        request.put("prompt", prompt);

        if (shortResponse) {

            url = hostBaseUrl + "/api/ai/short";

        } else {

            url = hostBaseUrl + "/api/ai/unstructured";

            request.put("context", context);
        }

        return post(url, request, String.class, true);
    }


    /**
     * Reads the HTTP response status code and body from the given connection.
     * <p>
     * If the server returns a successful No Content status ({@code 204}), this method returns
     * immediately with an empty body. For other statuses, it reads from the connection's standard
     * {@link java.io.InputStream InputStream} (for {@code 2xx} success codes) or the
     * {@link java.io.InputStream ErrorStream} (for non-{@code 2xx} error codes).
     * </p>
     *
     * @param conn the active {@link HttpURLConnection} to read the response from
     * @return a {@link HttpResponse} containing the HTTP status code and the raw response body
     * @throws Exception if an I/O error occurs while reading the response stream
     */
    private HttpResponse readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return new HttpResponse(status, "");
        }
        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        return new HttpResponse(status, response.toString());
    }
}