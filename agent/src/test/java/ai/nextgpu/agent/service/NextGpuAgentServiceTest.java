package ai.nextgpu.agent.service;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.agent.repository.ChatMessageRepository;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.WebSocketMessageDto;
import ai.nextgpu.common.util.JsonUtil;
import ai.nextgpu.common.model.PosthogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextGpuAgentServiceTest {

    @Mock private HttpUtil httpUtil;
    @Mock private GlobalPropertyRepository globalPropertyRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private DataService dataService;
    @Mock private WebSocketMessageService webSocketMessageService;
    @Mock private HardwareUtil hardwareUtil;
    @Mock private BenchmarkUtil benchmarkUtil;
    @Mock private ComputerService computerService;
    @Mock private AnalyticsService analyticsService;

    private NextGpuAgentService service;

    @BeforeEach
    void setUp() {
        // The constructor reads these properties on Windows (OSUtil.IS_WINDOWS == true),
        // and it dereferences them without null checks.
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO))
                .thenReturn(Optional.of(globalPropertyWithValue("Ubuntu")));
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME))
                .thenReturn(Optional.of(globalPropertyWithValue("user")));
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.OS_PASSWORD))
                .thenReturn(Optional.of(globalPropertyWithValue("pass")));
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.IS_SETUP_COMPLETED))
                .thenReturn(Optional.of(globalPropertyWithValue("false")));

        service = new NextGpuAgentService(
                globalPropertyRepository,
                dataService,
                webSocketMessageService,
                hardwareUtil,
                computerService,
                analyticsService
        );

        ReflectionTestUtils.setField(service, "SOCKET_URL", "ws://example.test/ws");
    }

    // -----------------------------
    // 4) initializeWebSocketClient()
    // -----------------------------

    @Test
    void initializeWebSocketClient_whenNotLoggedIn_invalidatesSessionKeyPair_andReturnsFalse() {
        ReflectionTestUtils.setField(service, "isLoggedIn", false);

        boolean result = service.initializeWebSocketClient("wallet-1", "jwt-1");

        assertFalse(result);
        verify(webSocketMessageService, times(1)).invalidateSessionKeyPair();
        verify(webSocketMessageService, never()).rotateSessionKeyPair();
    }

    @Test
    void initializeWebSocketClient_whenJwtMissing_returnsFalse_andDoesNotRotateKeys() {
        ReflectionTestUtils.setField(service, "isLoggedIn", true);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.empty());

        boolean result = service.initializeWebSocketClient("wallet-1", "jwt-1");

        assertFalse(result);
        verify(webSocketMessageService, never()).rotateSessionKeyPair();
        verify(webSocketMessageService, never()).invalidateSessionKeyPair();
    }

    @Test
    void initializeWebSocketClient_whenJwtEmpty_returnsFalse_andDoesNotRotateKeys() {
        ReflectionTestUtils.setField(service, "isLoggedIn", true);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(globalPropertyWithValue("")));

        boolean result = service.initializeWebSocketClient("wallet-1", "jwt-1");

        assertFalse(result);
        verify(webSocketMessageService, never()).rotateSessionKeyPair();
        verify(webSocketMessageService, never()).invalidateSessionKeyPair();
    }

    @Test
    void initializeWebSocketClient_happyPath_rotatesKeys_constructsClient_setsField_andReturnsTrue() {
        ReflectionTestUtils.setField(service, "isLoggedIn", true);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(globalPropertyWithValue("jwt-token")));

        try (MockedConstruction<WebSocketStompClient> mocked = mockConstruction(WebSocketStompClient.class)) {

            boolean result = service.initializeWebSocketClient("wallet-1", "jwt-1");

            assertTrue(result);

            verify(webSocketMessageService, times(1)).rotateSessionKeyPair();
            verify(webSocketMessageService, never()).invalidateSessionKeyPair();

            assertEquals(1, mocked.constructed().size(), "Expected exactly one WebSocketStompClient construction");
            assertNotNull(service.getWebSocketClient(), "Service should store constructed WebSocketStompClient");
        }
    }

    @Test
    void initializeWebSocketClient_whenRotateSessionKeyPairThrows_invalidatesSessionKeyPair_andReturnsFalse() {
        ReflectionTestUtils.setField(service, "isLoggedIn", true);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(globalPropertyWithValue("jwt-token")));

        doThrow(new RuntimeException("rotate failed")).when(webSocketMessageService).rotateSessionKeyPair();

        boolean result = service.initializeWebSocketClient("wallet-1", "jwt-1");

        assertFalse(result);
        verify(webSocketMessageService, times(1)).invalidateSessionKeyPair();
    }

    // ----------------------------------------
    // 5) sendWebSocketMessage(WebSocketMessageDto)
    // ----------------------------------------

    @Test
    void sendWebSocketMessage_whenWebSocketClientIsNull_doesNothing() {
        ReflectionTestUtils.setField(service, "webSocketClient", null);

        WebSocketMessageDto dto = new WebSocketMessageDto("u", "role", "hello", 1, false);

        assertDoesNotThrow(() -> service.sendWebSocketMessage(dto));
        verifyNoInteractions(webSocketMessageService);
    }

    @Test
    void sendWebSocketMessage_whenMessageJsonFalse_sendsAsIs_withoutEncryption() {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);

        WebSocketMessageDto dto = new WebSocketMessageDto("u", "role", "hello", 1, false);

        service.sendWebSocketMessage(dto);

        verify(client, times(1)).sendMessage(same(dto));
        verifyNoInteractions(webSocketMessageService);
    }

    @Test
    void sendWebSocketMessage_whenMessageJsonTrue_encryptsAndWrapsPayload_thenSends() throws Exception {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);

        byte[] aesKey = new byte[32];
        byte[] encapsulatedKey = new byte[] { 1, 2, 3 };
        byte[] encryptedMessage = new byte[] { 9, 8, 7 };

        when(webSocketMessageService.get32BytesAesKey()).thenReturn(aesKey);
        when(webSocketMessageService.getAesEncapsulatedKey()).thenReturn(encapsulatedKey);
        when(webSocketMessageService.encryptMessage(any(byte[].class), same(aesKey))).thenReturn(encryptedMessage);

        when(webSocketMessageService.isSessionConsumerPublicKeySet()).thenReturn(true);

        WebSocketMessageDto dto = new WebSocketMessageDto("u", "role", "hello", 1, true);

        service.sendWebSocketMessage(dto);

        verify(webSocketMessageService, times(1))
                .encryptMessage(eq("hello".getBytes(StandardCharsets.UTF_8)), same(aesKey));

        verify(client, times(1)).sendMessage(same(dto));

        assertNotNull(dto.getMessage(), "Message should be rewritten to JSON string");
        var json = JsonUtil.OBJECT_MAPPER.readTree(dto.getMessage());
        assertTrue(json.has("encryptedMessage"), "JSON payload should contain encryptedMessage");
        assertTrue(json.has("encapsulatedKey"), "JSON payload should contain encapsulatedKey");
        assertFalse(json.get("encryptedMessage").asText().isBlank(), "encryptedMessage should not be blank");
        assertFalse(json.get("encapsulatedKey").asText().isBlank(), "encapsulatedKey should not be blank");
    }

    @Test
    void sendWebSocketMessage_whenEncryptionFails_throwsRuntimeException() throws Exception {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);

        byte[] aesKey = new byte[32];
        when(webSocketMessageService.get32BytesAesKey()).thenReturn(aesKey);
        when(webSocketMessageService.getAesEncapsulatedKey()).thenReturn(new byte[] { 1, 2, 3 });

        when(webSocketMessageService.encryptMessage(any(byte[].class), same(aesKey)))
                .thenThrow(new GeneralSecurityException("encrypt failed"));

        when(webSocketMessageService.isSessionConsumerPublicKeySet()).thenReturn(true);

        WebSocketMessageDto dto = new WebSocketMessageDto("u", "role", "hello", 1, true);

        assertThrows(RuntimeException.class, () -> service.sendWebSocketMessage(dto));
        verify(client, never()).sendMessage(any());
    }

    @Test
    void testUpdateAppTheme() {
        service.updateAppTheme("dark");
        verify(analyticsService).captureEvent(eq(PosthogEvent.THEME_SELECTED.name()), eq(java.util.Map.of("theme", "dark")));
        verify(globalPropertyRepository).save(any(GlobalProperty.class));
    }

    @Test
    void testToggleAdvancedMode() {
        service.toggleAdvancedMode(true);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MODE_SELECTED.name()), eq(java.util.Map.of("mode", "advanced")));
        verify(globalPropertyRepository).save(any(GlobalProperty.class));

        service.toggleAdvancedMode(false);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MODE_SELECTED.name()), eq(java.util.Map.of("mode", "basic")));
    }

    private static GlobalProperty globalPropertyWithValue(String value) {
        GlobalProperty p = new GlobalProperty();
        p.setValueReference(value);
        p.setDatatype("java.lang.String");
        return p;
    }
}
