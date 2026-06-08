package ai.nextgpu.agent.util;

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
public class HttpUtilTest {

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

    @Test
    void testGet_withoutAuth() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\":\"ok\"}")));

        String url = "http://localhost:" + wireMock.getPort() + "/test";
        String response = httpUtil.get(url, (String) null);
        assertEquals("{\"status\":\"ok\"}", response);
    }

    @Test
    void testGet_withAuth() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/secure"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("secret")));

        String url = "http://localhost:" + wireMock.getPort() + "/secure";
        String response = httpUtil.get(url, "Bearer my-token");

        assertEquals("secret", response);
        wireMock.verify(getRequestedFor(urlEqualTo("/secure"))
                .withHeader("Authorization", equalTo("Bearer my-token")));
    }

    @Test
    void testPost_withBody() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/data"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{\"id\":1}")));

        Map<String, String> body = new HashMap<>();
        body.put("name", "test");

        String url = "http://localhost:" + wireMock.getPort() + "/data";
        
        // Using post(String, Object, Class, boolean) through public API
        // But wait, post(String, Object, String) is private.
        // I'll test it via the public generic post method.
        
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
            .thenReturn(Optional.of(createGlobalProperty(GlobalPropertyConfig.JWT_TOKEN, "token123")));

        Map response = httpUtil.post(url, body, Map.class, true);

        assertEquals(1, response.get("id"));
        wireMock.verify(postRequestedFor(urlEqualTo("/data"))
                .withHeader("Authorization", equalTo("Bearer token123"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("test"))));
    }

    private GlobalProperty createGlobalProperty(String name, String value) {
        GlobalProperty property = new GlobalProperty();
        property.setName(name);
        property.setValueReference(value);
        return property;
    }

    @Test
    void testReadResponse_withErrorStreamHandling() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("Bad Request")));

        String url = "http://localhost:" + wireMock.getPort() + "/error";
        String response = httpUtil.get(url, (String) null);

        assertEquals("Bad Request", response);
    }

    @Test
    void testDelete() throws Exception {
        wireMock.stubFor(delete(urlEqualTo("/resource/1"))
                .willReturn(aResponse().withStatus(204)));

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(createGlobalProperty(GlobalPropertyConfig.JWT_TOKEN, "token123")));

        String url = "http://localhost:" + wireMock.getPort() + "/resource/1";
        String response = httpUtil.delete(url);

        assertEquals("", response);
        wireMock.verify(deleteRequestedFor(urlEqualTo("/resource/1"))
                .withHeader("Authorization", equalTo("Bearer token123")));
    }
}
