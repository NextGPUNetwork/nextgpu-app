package ai.nextgpu.agent.util;

import ai.nextgpu.agent.exception.ApiException;
import ai.nextgpu.common.dto.ErrorResponseDto;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.service.BaseTest;
import ai.nextgpu.common.model.GlobalProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BaseTest.TestConfig.class)
@ExtendWith(MockitoExtension.class)
class HttpUtilTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Mock
    private GlobalPropertyRepository globalPropertyRepository;

    private HttpUtil httpUtil;

    @BeforeEach
    void setUp() {
        httpUtil = new HttpUtil(globalPropertyRepository);
    }

    private GlobalProperty createGlobalProperty(String name, String value) {
        GlobalProperty property = new GlobalProperty();
        property.setName(name);
        property.setValueReference(value);
        return property;
    }

    @Test
    void testGet_withoutAuth() throws Exception {

        wireMock.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));

        String url = "http://localhost:" + wireMock.getPort() + "/test";

        Map response = httpUtil.get(
                url,
                Map.class,
                false);

        assertEquals("ok", response.get("status"));
    }

    @Test
    void testGet_withAuth() throws Exception {

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(
                        createGlobalProperty(
                                GlobalPropertyConfig.JWT_TOKEN,
                                "token123")));

        wireMock.stubFor(get(urlEqualTo("/secure"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"secret\"}")));

        String url = "http://localhost:" + wireMock.getPort() + "/secure";

        Map response = httpUtil.get(
                url,
                Map.class,
                true);

        assertEquals("secret", response.get("message"));

        wireMock.verify(getRequestedFor(urlEqualTo("/secure"))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer token123")));
    }

    @Test
    void testPost_withBody() throws Exception {

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(
                        createGlobalProperty(
                                GlobalPropertyConfig.JWT_TOKEN,
                                "token123")));

        wireMock.stubFor(post(urlEqualTo("/data"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1}")));

        Map<String, String> body = new HashMap<>();
        body.put("name", "test");

        String url = "http://localhost:" + wireMock.getPort() + "/data";

        Map response = httpUtil.post(
                url,
                body,
                Map.class,
                true);

        assertEquals(1, response.get("id"));

        wireMock.verify(postRequestedFor(urlEqualTo("/data"))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer token123"))
                .withRequestBody(
                        matchingJsonPath("$.name", equalTo("test"))));
    }

    @Test
    void testGet_shouldThrowApiException() {

        wireMock.stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "timestamp":"2026-07-14T10:00:00Z",
                              "status":400,
                              "error":"Bad Request",
                              "errorCode":"UNKNOWN_ERROR",
                              "message":"Invalid request",
                              "path":"/error"
                            }
                            """)));

        String url = "http://localhost:" + wireMock.getPort() + "/error";

        ApiException exception = assertThrows(
                ApiException.class,
                () -> httpUtil.get(
                        url,
                        Map.class,
                        false));

        ErrorResponseDto error =
                exception.getErrorResponse();

        assertEquals("Invalid request", error.getMessage());
    }

    @Test
    void testPost_shouldThrowApiException() {

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(
                        createGlobalProperty(
                                GlobalPropertyConfig.JWT_TOKEN,
                                "token123")));

        wireMock.stubFor(post(urlEqualTo("/data"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "timestamp":"2026-07-14T10:00:00Z",
                              "status":500,
                              "error":"Internal Server Error",
                              "errorCode":"UNKNOWN_ERROR",
                              "message":"Database unavailable",
                              "path":"/data"
                            }
                            """)));

        String url = "http://localhost:" + wireMock.getPort() + "/data";

        ApiException exception = assertThrows(
                ApiException.class,
                () -> httpUtil.post(
                        url,
                        Map.of("name", "test"),
                        Map.class,
                        true));

        assertEquals(
                "Database unavailable",
                exception.getErrorResponse().getMessage());
    }

    @Test
    void testDelete() throws Exception {

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(
                        createGlobalProperty(
                                GlobalPropertyConfig.JWT_TOKEN,
                                "token123")));

        wireMock.stubFor(delete(urlEqualTo("/resource/1"))
                .willReturn(aResponse()
                        .withStatus(204)));

        String url =
                "http://localhost:" +
                        wireMock.getPort() +
                        "/resource/1";

        httpUtil.delete(url);

        wireMock.verify(deleteRequestedFor(
                urlEqualTo("/resource/1"))
                .withHeader(
                        "Authorization",
                        equalTo("Bearer token123")));
    }

    @Test
    void testDelete_shouldThrowApiException() {

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(
                        createGlobalProperty(
                                GlobalPropertyConfig.JWT_TOKEN,
                                "token123")));

        wireMock.stubFor(delete(urlEqualTo("/resource/1"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "timestamp":"2026-07-14T10:00:00Z",
                              "status":404,
                              "error":"Not Found",
                              "errorCode":"UNKNOWN_ERROR",
                              "message":"Resource not found",
                              "path":"/resource/1"
                            }
                            """)));

        String url =
                "http://localhost:" +
                        wireMock.getPort() +
                        "/resource/1";

        ApiException exception = assertThrows(
                ApiException.class,
                () -> httpUtil.delete(url));

        assertEquals(
                "Resource not found",
                exception.getErrorResponse().getMessage());
    }
}
