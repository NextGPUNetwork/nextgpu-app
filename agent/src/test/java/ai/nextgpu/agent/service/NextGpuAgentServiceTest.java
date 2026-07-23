package ai.nextgpu.agent.service;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.common.dto.*;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.report.HardwareReport;
import ai.nextgpu.common.util.JsonUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
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

    @Mock private GlobalPropertyRepository globalPropertyRepository;
    @Mock private DataService dataService;
    @Mock private WebSocketMessageService webSocketMessageService;
    @Mock private HardwareUtil hardwareUtil;
    @Mock private ComputerService computerService;
    @Mock private AnalyticsService analyticsService;
    @Mock private ProviderService providerService;
    @Mock private NextGpuWebService nextGpuWebService;

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
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.IS_SETUP_COMPLETED))
                .thenReturn(Optional.of(globalPropertyWithValue("false")));
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOCAL_IP))
                .thenReturn(Optional.of(globalPropertyWithValue("")));

        service = spy(new NextGpuAgentService(
                globalPropertyRepository,
                dataService,
                webSocketMessageService,
                hardwareUtil,
                computerService,
                analyticsService,
                providerService
        ));

        ReflectionTestUtils.setField(service, "SOCKET_URL", "ws://example.test/ws");
        ReflectionTestUtils.setField(service, "nextGpuWebService", nextGpuWebService);

        // Clear invocations from constructor's checkEnvironment() so verify counts start fresh
        clearInvocations(globalPropertyRepository);

        lenient().when(globalPropertyRepository.save(any(GlobalProperty.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GlobalProperty loginWalletProperty = new GlobalProperty();
        String testWallet = "test-wallet-address";
        loginWalletProperty.setValueReference(testWallet);
    }

    // ================================
    // Global Property Tests
    // ================================

    @Test
    void getGlobalProperty_whenExists_returnsProperty() {
        GlobalProperty expected = globalPropertyWithValue("test-value");
        when(globalPropertyRepository.findByName("test-name")).thenReturn(Optional.of(expected));

        GlobalProperty result = service.getGlobalProperty("test-name");

        assertNotNull(result);
        assertEquals("test-value", result.getValueReference());
    }

    @Test
    void getGlobalProperty_whenNotExists_returnsNull() {
        when(globalPropertyRepository.findByName("non-existent")).thenReturn(Optional.empty());

        GlobalProperty result = service.getGlobalProperty("non-existent");

        assertNull(result);
    }

    @Test
    void saveGlobalProperty_withNameValueDatatype_existingProperty_updatesValue() {
        GlobalProperty existing = globalPropertyWithValue("old-value");
        existing.setName("test-name");
        when(globalPropertyRepository.findByName("test-name")).thenReturn(Optional.of(existing));

        service.saveGlobalProperty("test-name", "new-value", "java.lang.String");

        verify(globalPropertyRepository).save(existing);
        assertEquals("new-value", existing.getValueReference());
    }

    @Test
    void saveGlobalProperty_withNameValueDatatype_newProperty_createsNew() {
        when(globalPropertyRepository.findByName("new-name")).thenReturn(Optional.empty());

        service.saveGlobalProperty("new-name", "new-value", "java.lang.String");

        verify(globalPropertyRepository).save(argThat(prop ->
                prop.getName().equals("new-name") &&
                        prop.getValueReference().equals("new-value") &&
                        prop.getDatatype().equals("java.lang.String")
        ));
    }

    @Test
    void saveGlobalProperty_withObject_savesAndReturns() {
        GlobalProperty property = globalPropertyWithValue("test");
        when(globalPropertyRepository.save(property)).thenReturn(property);

        GlobalProperty result = service.saveGlobalProperty(property);

        assertEquals(property, result);
        verify(globalPropertyRepository).save(property);
    }

    @Test
    void saveSetupCompletionGlobalProperties_updatesBothProperties() {
        GlobalProperty setupProp = globalPropertyWithValue("false");
        setupProp.setName(GlobalPropertyConfig.IS_SETUP_COMPLETED);
        GlobalProperty versionProp = globalPropertyWithValue("1");
        versionProp.setName(GlobalPropertyConfig.SETUP_VERSION);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.IS_SETUP_COMPLETED))
                .thenReturn(Optional.of(setupProp));
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.SETUP_VERSION))
                .thenReturn(Optional.of(versionProp));

        service.saveSetupCompletionGlobalProperties(true, 2);

        assertEquals("true", setupProp.getValueReference());
        assertEquals("2", versionProp.getValueReference());
        verify(globalPropertyRepository, times(2)).save(any(GlobalProperty.class));
    }

    // ================================
    // 4) initializeWebSocketClient()
    // ================================

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
    void sendWebSocketMessage_withStringMessage_sendsWithPublicKey() {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);
        when(client.getUsername()).thenReturn("test-user");
        when(webSocketMessageService.getMyPublicKeyBase64()).thenReturn("public-key-base64");

        service.sendWebSocketMessage("test message", 100);

        verify(client).sendMessage(argThat(dto ->
                dto.getMessage().equals("test message") &&
                        dto.getMessageCode() == 100 &&
                        dto.getPublicKey().equals("public-key-base64")
        ));
    }

    @Test
    void testUpdateAppTheme() {
        service.updateAppTheme("dark");
        verify(analyticsService).captureEvent(eq(PosthogEvent.THEME_SELECTED.name()), eq(Map.of("theme", "dark")));
        verify(globalPropertyRepository).save(any(GlobalProperty.class));
    }

    @Test
    void testToggleAdvancedMode() {
        service.toggleAdvancedMode(true);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MODE_SELECTED.name()), eq(Map.of("mode", "advanced")));
        verify(globalPropertyRepository).save(any(GlobalProperty.class));

        service.toggleAdvancedMode(false);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MODE_SELECTED.name()), eq(Map.of("mode", "basic")));
    }

    // ================================
    // Authentication Tests
    // ================================

    @Test
    void authenticate_happyPath_returnsTrue() throws Exception {
        AuthResponseDto authResponse = new AuthResponseDto();
        UserDto userDto = new UserDto();
        userDto.setRole(Role.PROVIDER);
        userDto.setWalletAddress("0x123");
        authResponse.setUserDto(userDto);
        authResponse.setAccessToken("jwt-token");

        ComputerAttributeTypeDto computerAttributeTypeDto = new ComputerAttributeTypeDto();
        computerAttributeTypeDto.setName("registration_status");

        ProviderAttributeTypeDto providerAttributeTypeDto = new ProviderAttributeTypeDto();
        providerAttributeTypeDto.setName("min_stake_status");


        when(nextGpuWebService.verifyOtp(anyString())).thenReturn(authResponse);
        when(nextGpuWebService.getComputerAttributeTypes()).thenReturn(List.of(computerAttributeTypeDto));
        when(nextGpuWebService.getProviderAttributeTypes()).thenReturn(List.of(providerAttributeTypeDto));

        Provider provider = new Provider();
        provider.setWalletAddress("0x123");
        when(providerService.saveProvider(any(UserDto.class))).thenReturn(provider);

        boolean result = service.authenticate("otp-123");

        assertTrue(result);
        verify(globalPropertyRepository, atLeastOnce()).save(any(GlobalProperty.class));
        verify(providerService).saveProvider(userDto);
    }

    @Test
    void authenticate_whenVerifyOtpThrows_throwsRuntimeException() throws Exception {
        when(nextGpuWebService.verifyOtp(anyString())).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class, () -> service.authenticate("otp-123"));
        verify(webSocketMessageService).invalidateSessionKeyPair();
    }

    @Test
    void authenticate_whenUserNotProvider_returnsFalse() throws Exception {
        AuthResponseDto authResponse = new AuthResponseDto();
        UserDto userDto = new UserDto();
        userDto.setRole(Role.CONSUMER);
        authResponse.setUserDto(userDto);

        when(nextGpuWebService.verifyOtp(anyString())).thenReturn(authResponse);

        boolean result = service.authenticate("otp-123");

        assertFalse(result);
        verify(providerService, never()).saveProvider(any());
    }

    // ================================
    // Logout Tests
    // ================================

    @Test
    void logout_disconnectsAndClearsToken() {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(globalPropertyWithValue("0x123")));

        service.logout();

        verify(client).disconnect("0x123");
        verify(webSocketMessageService).invalidateSessionKeyPair();
        assertNull(service.getWebSocketClient());
    }

    @Test
    void logout_whenClientIsNull_doesNothing() {
        ReflectionTestUtils.setField(service, "webSocketClient", null);

        service.logout();

        verify(webSocketMessageService, never()).invalidateSessionKeyPair();
        verify(globalPropertyRepository, never()).save(any());
    }

    @Test
    void logout_whenDisconnectThrows_stillClearsToken() {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(globalPropertyWithValue("0x123")));

        doThrow(new RuntimeException("disconnect failed")).when(client).disconnect(anyString());

        service.logout();

        verify(webSocketMessageService).invalidateSessionKeyPair();
        verify(globalPropertyRepository).save(argThat(p -> p.getName().equals(GlobalPropertyConfig.JWT_TOKEN) && p.getValueReference() == null));
    }

    // ================================
    // Hardware Detection Tests
    // ================================



    @Test
    void checkAndSaveNvidiaGpuPresence_noGpu_savesFalse() {
        when(hardwareUtil.detectGpus()).thenReturn(Collections.emptySet());

        boolean result = service.checkAndSaveNvidiaGpuPresence();

        assertFalse(result);
        verify(globalPropertyRepository).save(argThat(p -> p.getName().equals(GlobalPropertyConfig.HAS_NVIDIA_GPU) && p.getValueReference().equals("false")));
    }

    @Test
    void checkAndSaveNvidiaGpuPresence_nonNvidiaGpu_savesFalse() {
        Gpu gpu = new Gpu();
        gpu.setManufacturer("AMD");
        when(hardwareUtil.detectGpus()).thenReturn(Collections.singleton(gpu));

        boolean result = service.checkAndSaveNvidiaGpuPresence();

        assertFalse(result);
        verify(globalPropertyRepository).save(argThat(p -> p.getName().equals(GlobalPropertyConfig.HAS_NVIDIA_GPU) && p.getValueReference().equals("false")));
    }

    @Test
    void checkAndSaveNvidiaGpuPresence_detectsNvidia() {
        Gpu gpu = new Gpu();
        gpu.setManufacturer("NVIDIA Corporation");

        when(hardwareUtil.detectGpus()).thenReturn(Collections.singleton(gpu));

        boolean result = service.checkAndSaveNvidiaGpuPresence();

        assertTrue(result);
        verify(globalPropertyRepository).save(argThat(p -> p.getName().equals(GlobalPropertyConfig.HAS_NVIDIA_GPU) && p.getValueReference().equals("true")));
    }


    @Test
    void checkAndSaveNvidiaGpuPresence_gpusNull_savesFalse() {
        when(hardwareUtil.detectGpus()).thenReturn(null);

        boolean result = service.checkAndSaveNvidiaGpuPresence();

        assertFalse(result);
        verify(globalPropertyRepository).save(argThat(p -> p.getName().equals(GlobalPropertyConfig.HAS_NVIDIA_GPU) && p.getValueReference().equals("false")));
    }

    @Test
    void detectHardware_delegatesToHardwareUtil() {
        Computer expected = new Computer();
        when(hardwareUtil.detectComputer()).thenReturn(expected);

        Computer result = service.detectHardware();

        assertEquals(expected, result);
        verify(hardwareUtil).detectComputer();
    }

    @Test
    void generateComputerBenchmarkReport_delegatesToComputerService() throws Exception {
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));
        BenchmarkReport expectedReport = new BenchmarkReport();
        when(computerService.generateComputerBenchmarkReport("0x123", false))
                .thenReturn(expectedReport);

        BenchmarkReport result = service.generateComputerBenchmarkReport(false);

        assertEquals(expectedReport, result);
        verify(computerService).generateComputerBenchmarkReport("0x123", false);
    }

    @Test
    void generateComputerUsageReport_delegatesToComputerService() {
        Map<String, Object> expectedReport = Map.of("key", "value");
        when(computerService.generateComputerUsageReport()).thenReturn(expectedReport);

        Map<String, Object> result = service.generateComputerUsageReport();

        assertEquals(expectedReport, result);
        verify(computerService).generateComputerUsageReport();
    }

    @Test
    void generateComputerHardwareReport_delegatesToComputerService() throws Exception {
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));
        HardwareReport expectedReport = new HardwareReport();
        when(computerService.generateComputerHardwareReport("0x123", true))
                .thenReturn(expectedReport);

        HardwareReport result = service.generateComputerHardwareReport(true);

        assertEquals(expectedReport, result);
        verify(computerService).generateComputerHardwareReport("0x123", true);
    }

    // ================================
    // Provider Tests
    // ================================

    @Test
    void saveOSCredentials_savesToRepository() {
        boolean result = service.saveOSCredentials("user", "pass");

        assertTrue(result);
        verify(globalPropertyRepository, times(2)).save(any(GlobalProperty.class));
    }

    @Test
    void saveOSCredentials_whenException_returnsFalse() {
        doThrow(new RuntimeException("save failed")).when(globalPropertyRepository).save(any(GlobalProperty.class));

        boolean result = service.saveOSCredentials("user", "pass");

        assertFalse(result);
    }

    @Test
    void nukeInstance_purgesEverything() {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(globalPropertyWithValue("0x123")));

        boolean result = service.nukeInstance();

        assertTrue(result);
        verify(dataService).purgeDatabase();
        verify(webSocketMessageService).invalidateSessionKeyPair();
    }

    @Test
    void nukeInstance_whenException_returnsFalse() {
        WebSocketStompClient client = mock(WebSocketStompClient.class);
        ReflectionTestUtils.setField(service, "webSocketClient", client);
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(globalPropertyWithValue("0x123")));
        doThrow(new RuntimeException("purge failed")).when(dataService).purgeDatabase();

        boolean result = service.nukeInstance();

        assertFalse(result);
    }

    // ================================
    // Application Settings Tests
    // ================================

    @Test
    void togglePrivateMode_savesSetting() {
        service.togglePrivateMode(true);
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.IS_PRIVATE_MODE) &&
                        p.getValueReference().equals("true")
        ));

        service.togglePrivateMode(false);
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.IS_PRIVATE_MODE) &&
                        p.getValueReference().equals("false")
        ));
    }

    @Test
    void updateIsOpenclawSetupCompleteProperty_savesSetting() {
        service.updateIsOpenclawSetupCompleteProperty(true);
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.IS_OPENCLAW_SETUP_COMPLETED) &&
                        p.getValueReference().equals("true")
        ));

        service.updateIsOpenclawSetupCompleteProperty(false);
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.IS_OPENCLAW_SETUP_COMPLETED) &&
                        p.getValueReference().equals("false")
        ));
    }

    @Test
    void toggleOpenclawShortcut_savesSetting() {
        service.toggleOpenclawShortcut(true);
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.SHOW_OPENCLAW_SHORTCUT) &&
                        p.getValueReference().equals("true")
        ));

        service.toggleOpenclawShortcut(false);
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.SHOW_OPENCLAW_SHORTCUT) &&
                        p.getValueReference().equals("false")
        ));
    }

    @Test
    void updateInstallProfile_savesProfile() {
        service.updateInstallProfile("provider");
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.INSTALL_PROFILE) &&
                        p.getValueReference().equals("provider")
        ));
    }

    @Test
    void updateLastActiveScreen_savesScreen() {
        service.updateLastActiveScreen("dashboard");
        verify(globalPropertyRepository).save(argThat(p ->
                p.getName().equals(GlobalPropertyConfig.LAST_ACTIVE_SCREEN) &&
                        p.getValueReference().equals("dashboard")
        ));
    }

    @Test
    void getLastActiveScreen_whenSet_returnsScreen() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LAST_ACTIVE_SCREEN))
                .thenReturn(Optional.of(globalPropertyWithValue("settings")));

        String result = service.getLastActiveScreen();

        assertEquals("settings", result);
    }

    @Test
    void getLastActiveScreen_whenNotSet_returnsEmptyString() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LAST_ACTIVE_SCREEN))
                .thenReturn(Optional.empty());

        String result = service.getLastActiveScreen();

        assertEquals("", result);
    }

    @Test
    void getLastActiveScreen_whenNullValue_returnsEmptyString() {
        GlobalProperty prop = new GlobalProperty();
        prop.setValueReference(null);
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LAST_ACTIVE_SCREEN))
                .thenReturn(Optional.of(prop));

        String result = service.getLastActiveScreen();

        assertEquals("", result);
    }


    // ================================
    // fetchAndUpdateComputer() tests
    // ================================

    @Test
    void fetchAndUpdateComputer_happyPath_returnsUpdatedComputer() throws Exception {
        ComputerDto fetchedDto = new ComputerDto();
        fetchedDto.setUuid("comp-uuid");
        Computer updatedComputer = new Computer();
        updatedComputer.setUuid("comp-uuid");

        when(nextGpuWebService.getComputer("comp-uuid")).thenReturn(fetchedDto);
        when(computerService.updateComputer(fetchedDto)).thenReturn(updatedComputer);

        Computer result = service.fetchAndUpdateComputer("comp-uuid");

        assertNotNull(result);
        assertEquals("comp-uuid", result.getUuid());
        verify(nextGpuWebService).getComputer("comp-uuid");
        verify(computerService).updateComputer(fetchedDto);
    }

    @Test
    void fetchAndUpdateComputer_whenWebServiceThrows_throwsRuntimeException() throws Exception {
        when(nextGpuWebService.getComputer(anyString())).thenThrow(new RuntimeException("not found"));

        assertThrows(RuntimeException.class, () -> service.fetchAndUpdateComputer("comp-uuid"));
        verify(computerService, never()).updateComputer(any());
    }

    // ================================
    // applyAsAProvider() tests
    // ================================

    @Test
    void applyAsAProvider_whenNotLoggedIn_throwsRuntimeException() {
        doReturn(false).when(service).hasValidJwt();

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));
        verifyNoInteractions(hardwareUtil, computerService, nextGpuWebService);
    }

    @Test
    void applyAsAProvider_whenLoginWalletMissing_throwsRuntimeExceptionBeforeDetectingHardware() {
        doReturn(true).when(service).hasValidJwt();
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));

        verifyNoInteractions(hardwareUtil, computerService, nextGpuWebService);
    }

    @Test
    void applyAsAProvider_happyPath_auditPassesReturnsTrue() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport(eq("0x123"), anyBoolean())).thenReturn(benchmarkReport);
        when(nextGpuWebService.auditComputerForAnomalies(any(ComputerDto.class))).thenReturn(true);
        when(nextGpuWebService.getComputer("comp-uuid")).thenReturn(new ComputerDto());
        when(computerService.updateComputer(any(ComputerDto.class))).thenReturn(savedComputer);

        List<String> statuses = new ArrayList<>();

        boolean result = service.applyAsAProvider(statuses::add);

        assertTrue(result);
        assertTrue(statuses.contains("Detecting provider hardware"), "Statuses were: " + statuses);
        assertTrue(statuses.contains("Generating hardware benchmark report"), "Statuses were: " + statuses);
        assertTrue(statuses.contains("Auditing computer for anomalies"), "Statuses were: " + statuses);

    }

    @Test
    void applyAsAProvider_happyPath_auditFailsReturnsFalse() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport(eq("0x123"), anyBoolean())).thenReturn(benchmarkReport);
        when(nextGpuWebService.auditComputerForAnomalies(any(ComputerDto.class))).thenReturn(false);

        List<String> statuses = new ArrayList<>();

        boolean result = service.applyAsAProvider(statuses::add);

        assertFalse(result);
        assertTrue(statuses.contains("Detecting provider hardware"));
        assertTrue(statuses.contains("Generating hardware benchmark report"));
        assertTrue(statuses.contains("Auditing computer for anomalies"));

        // fetchAndUpdateComputer should not be called when audit fails
        verify(nextGpuWebService, never()).getComputer(any());
    }

    @Test
    void applyAsAProvider_generatesFullBenchmarkReport() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport("0x123", true)).thenReturn(benchmarkReport);
        when(nextGpuWebService.auditComputerForAnomalies(any(ComputerDto.class))).thenReturn(false);

        service.applyAsAProvider(status -> {});

        verify(computerService).generateComputerBenchmarkReport("0x123", true);
        verify(computerService, never()).generateComputerBenchmarkReport("0x123", false);
    }

    @Test
    void applyAsAProvider_whenDetectHardwareThrows_throwsRuntimeException() throws Exception{
        doReturn(true).when(service).hasValidJwt();

        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        when(hardwareUtil.detectComputer()).thenThrow(new RuntimeException("detection failed"));

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));
        verify(computerService, never()).saveComputer(any(), any());

    }

    @Test
    void applyAsAProvider_whenBenchmarkGenerationThrows_throwsRuntimeException() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport("0x123", true))
                .thenThrow(new RuntimeException("benchmark failed"));

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));
        verify(nextGpuWebService, never()).saveBenchmarkReport(any());
    }

    @Test
    void applyAsAProvider_whenSaveBenchmarkReportThrows_throwsRuntimeException() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport("0x123", true)).thenReturn(benchmarkReport);
        doThrow(new RuntimeException("save failed")).when(nextGpuWebService).saveBenchmarkReport(any());

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));
        verify(nextGpuWebService, never()).auditComputerForAnomalies(any());
    }

    @Test
    void applyAsAProvider_whenAuditThrows_throwsRuntimeException() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport("0x123", true)).thenReturn(benchmarkReport);
        when(nextGpuWebService.auditComputerForAnomalies(any(ComputerDto.class)))
                .thenThrow(new RuntimeException("audit failed"));

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));
        verify(nextGpuWebService, never()).getComputer(any());
    }

    @Test
    void applyAsAProvider_whenFetchAndUpdateThrows_throwsRuntimeException() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport("0x123", true)).thenReturn(benchmarkReport);
        when(nextGpuWebService.auditComputerForAnomalies(any(ComputerDto.class))).thenReturn(true);
        when(nextGpuWebService.getComputer("comp-uuid")).thenThrow(new RuntimeException("fetch failed"));

        assertThrows(RuntimeException.class, () -> service.applyAsAProvider(status -> {}));
    }

    @Test
    void applyAsAProvider_verifiesFullWorkflowInOrder() throws Exception {
        doReturn(true).when(service).hasValidJwt();

        // Mock the LOGIN_WALLET property for saveDetectedHardware
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Computer savedComputer = buildSavedComputer("comp-uuid");
        BenchmarkReport benchmarkReport = buildBenchmarkReport("0x123", "comp-uuid");

        when(hardwareUtil.detectComputer()).thenReturn(new Computer());
        when(computerService.saveComputer(any(Computer.class), eq("0x123"))).thenReturn(savedComputer);
        when(computerService.generateComputerBenchmarkReport(eq("0x123"), anyBoolean())).thenReturn(benchmarkReport);
        when(nextGpuWebService.auditComputerForAnomalies(any(ComputerDto.class))).thenReturn(true);
        when(nextGpuWebService.getComputer("comp-uuid")).thenReturn(new ComputerDto());
        when(computerService.updateComputer(any(ComputerDto.class))).thenReturn(savedComputer);

        service.applyAsAProvider(status -> {});

        InOrder inOrder = inOrder(hardwareUtil, computerService, nextGpuWebService);
        inOrder.verify(hardwareUtil).detectComputer();
        inOrder.verify(computerService).saveComputer(any(), eq("0x123"));
        inOrder.verify(computerService).generateComputerBenchmarkReport(eq("0x123"), eq(true));
        inOrder.verify(nextGpuWebService).saveBenchmarkReport(any());
        inOrder.verify(nextGpuWebService).auditComputerForAnomalies(any());
        inOrder.verify(nextGpuWebService).getComputer("comp-uuid");
        inOrder.verify(computerService).updateComputer(any());
    }

    // ================================
    // JWT Validation Tests
    // ================================

    @Test
    void isJwtValid_validToken_returnsTrue() throws Exception {
        // Create a valid JWT token with future expiration
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        long futureExp = LocalDateTime.now(ZoneOffset.UTC).plusHours(1).toEpochSecond(ZoneOffset.UTC);
        String payload = Base64.getUrlEncoder().encodeToString(
                ("{\"sub\":\"0x123\",\"exp\":" + futureExp + "}").getBytes(StandardCharsets.UTF_8)
        );
        String signature = "signature";
        String token = header + "." + payload + "." + signature;

        boolean result = service.isJwtValid(token, "0x123");

        assertTrue(result);
    }

    @Test
    void isJwtValid_expiredToken_returnsFalse() throws Exception {
        // Create a JWT token with past expiration
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        long pastExp = LocalDateTime.now(ZoneOffset.UTC).minusHours(1).toEpochSecond(ZoneOffset.UTC);
        String payload = Base64.getUrlEncoder().encodeToString(
                ("{\"sub\":\"0x123\",\"exp\":" + pastExp + "}").getBytes(StandardCharsets.UTF_8)
        );
        String signature = "signature";
        String token = header + "." + payload + "." + signature;

        boolean result = service.isJwtValid(token, "0x123");

        assertFalse(result);
    }

    @Test
    void isJwtValid_wrongSubject_returnsFalse() throws Exception {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        long futureExp = LocalDateTime.now(ZoneOffset.UTC).plusHours(1).toEpochSecond(ZoneOffset.UTC);
        String payload = Base64.getUrlEncoder().encodeToString(
                ("{\"sub\":\"0x456\",\"exp\":" + futureExp + "}").getBytes(StandardCharsets.UTF_8)
        );
        String signature = "signature";
        String token = header + "." + payload + "." + signature;

        boolean result = service.isJwtValid(token, "0x123");

        assertFalse(result);
    }

    @Test
    void isJwtValid_nullToken_returnsFalse() {
        boolean result = service.isJwtValid(null, "0x123");
        assertFalse(result);
    }

    @Test
    void isJwtValid_emptyToken_returnsFalse() {
        boolean result = service.isJwtValid("", "0x123");
        assertFalse(result);
    }

    @Test
    void isJwtValid_blankToken_returnsFalse() {
        boolean result = service.isJwtValid("   ", "0x123");
        assertFalse(result);
    }

    @Test
    void isJwtValid_nullWallet_returnsFalse() throws Exception {
        String token = createValidToken("0x123");
        boolean result = service.isJwtValid(token, null);
        assertFalse(result);
    }

    @Test
    void isJwtValid_blankWallet_returnsFalse() throws Exception {
        String token = createValidToken("0x123");
        boolean result = service.isJwtValid(token, "   ");
        assertFalse(result);
    }

    @Test
    void isJwtValid_malformedToken_returnsFalse() {
        boolean result = service.isJwtValid("not.a.jwt.token", "0x123");
        assertFalse(result);
    }

    @Test
    void isJwtValid_noExpiration_returnsFalse() throws Exception {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().encodeToString(
                "{\"sub\":\"0x123\"}".getBytes(StandardCharsets.UTF_8)
        );
        String signature = "signature";
        String token = header + "." + payload + "." + signature;

        boolean result = service.isJwtValid(token, "0x123");

        assertFalse(result);
    }

    @Test
    void hasValidJwt_validToken_returnsTrue() {
        GlobalProperty jwtProp = globalPropertyWithValue("valid-token");
        jwtProp.setName(GlobalPropertyConfig.JWT_TOKEN);
        GlobalProperty walletProp = globalPropertyWithValue("0x123");
        walletProp.setName(GlobalPropertyConfig.LOGIN_WALLET);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(jwtProp));
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        doReturn(true).when(service).isJwtValid("valid-token", "0x123");

        boolean result = service.hasValidJwt();

        assertTrue(result);
    }

    @Test
    void hasValidJwt_missingToken_returnsFalse() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.empty());
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(globalPropertyWithValue("0x123")));

        boolean result = service.hasValidJwt();

        assertFalse(result);
    }

    @Test
    void hasValidJwt_missingWallet_returnsFalse() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.of(globalPropertyWithValue("token")));
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.empty());

        boolean result = service.hasValidJwt();

        assertFalse(result);
    }

    // ================================
    // Local Entity Fetchers Tests
    // ================================

    @Test
    void getLocalProvider_whenWalletExists_returnsProvider() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(globalPropertyWithValue("0x123")));
        Provider expectedProvider = new Provider();
        expectedProvider.setWalletAddress("0x123");
        when(providerService.getProviderByWalletAddress("0x123")).thenReturn(expectedProvider);

        Provider result = service.getLocalProvider();

        assertNotNull(result);
        assertEquals("0x123", result.getWalletAddress());
    }

    @Test
    void getLocalProvider_whenWalletMissing_returnsNull() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.empty());

        Provider result = service.getLocalProvider();

        assertNull(result);
    }

    @Test
    void getLocalProvider_whenWalletBlank_returnsNull() {
        GlobalProperty walletProp = globalPropertyWithValue("");
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET))
                .thenReturn(Optional.of(walletProp));

        Provider result = service.getLocalProvider();

        assertNull(result);
    }

    @Test
    void getLocalComputer_whenUuidExists_returnsComputer() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.COMPUTER_UUID))
                .thenReturn(Optional.of(globalPropertyWithValue("comp-uuid")));
        Computer expectedComputer = new Computer();
        expectedComputer.setUuid("comp-uuid");
        when(dataService.findComputerByUuid("comp-uuid")).thenReturn(Optional.of(expectedComputer));

        Computer result = service.getLocalComputer();

        assertNotNull(result);
        assertEquals("comp-uuid", result.getUuid());
    }

    @Test
    void getLocalComputer_whenUuidMissing_returnsNull() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.COMPUTER_UUID))
                .thenReturn(Optional.empty());

        Computer result = service.getLocalComputer();

        assertNull(result);
    }

    @Test
    void loggedIn_returnsIsLoggedInField() {
        ReflectionTestUtils.setField(service, "isLoggedIn", true);
        assertTrue(service.loggedIn());

        ReflectionTestUtils.setField(service, "isLoggedIn", false);
        assertFalse(service.loggedIn());
    }

    // ================================
    // Helpers
    // ================================

    private static Computer buildSavedComputer(String uuid) {
        Computer c = new Computer();
        c.setUuid(uuid);
        c.setCpus(Collections.emptySet());
        c.setGpus(Collections.emptySet());
        c.setMemories(Collections.emptySet());
        c.setStorages(Collections.emptySet());
        c.setNetworkDevices(Collections.emptySet());
        c.setOtherComponents(Collections.emptySet());
        c.setComputerAttributes(Collections.emptyMap());
        return c;
    }

    private static BenchmarkReport buildBenchmarkReport(String walletAddress, String computerUuid) {
        BenchmarkReport report = new BenchmarkReport();
        Provider provider = new Provider();
        provider.setWalletAddress(walletAddress);
        report.setProvider(provider);
        Computer computer = new Computer();
        computer.setUuid(computerUuid);
        report.setComputer(computer);
        return report;
    }

    private static GlobalProperty globalPropertyWithValue(String value) {
        GlobalProperty p = new GlobalProperty();
        p.setValueReference(value);
        p.setDatatype("java.lang.String");
        return p;
    }

    private String createValidToken(String subject) throws Exception {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        long futureExp = LocalDateTime.now(ZoneOffset.UTC).plusHours(1).toEpochSecond(ZoneOffset.UTC);
        String payload = Base64.getUrlEncoder().encodeToString(
                ("{\"sub\":\"" + subject + "\",\"exp\":" + futureExp + "}").getBytes(StandardCharsets.UTF_8)
        );
        String signature = "signature";
        return header + "." + payload + "." + signature;
    }
}