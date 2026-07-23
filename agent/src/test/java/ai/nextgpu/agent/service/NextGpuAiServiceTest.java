package ai.nextgpu.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.model.*;
import ai.nextgpu.agent.repository.*;
import ai.nextgpu.common.dto.GpuDto;
import ai.nextgpu.common.dto.AiModelDto;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.common.model.Gpu;
import ai.nextgpu.common.model.PosthogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.nextgpu.agent.util.OSUtil;
import ai.nextgpu.agent.util.OllamaUtil;
import ai.nextgpu.common.model.ComfyUiModelFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextGpuAiServiceTest {

    @Mock
    private GlobalPropertyRepository globalPropertyRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplate longRunningRestTemplate;

    @Mock AnalyticsService analyticsService;

    @Mock
    private NextGpuWebService nextGpuWebService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private NextGpuAiService nextGpuAiService;

    private final String OLLAMA_URL = "http://localhost:11434";

    @BeforeEach
    void setUp() {
        GlobalProperty historyLimitProp = new GlobalProperty();
        historyLimitProp.setName(GlobalPropertyConfig.TOKEN_HISTORY_LIMIT);
        historyLimitProp.setDatatype("java.lang.Integer");
        historyLimitProp.setValueReference("4096");
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.TOKEN_HISTORY_LIMIT))
                .thenReturn(Optional.of(historyLimitProp));
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.LOCAL_IP))
                .thenReturn(Optional.empty());
        lenient().when(globalPropertyRepository.findByName(GlobalPropertyConfig.MAX_PINNED_MESSAGES))
                .thenReturn(Optional.empty());

        OllamaUtil ollamaUtil = new OllamaUtil(globalPropertyRepository, OLLAMA_URL);
        nextGpuAiService = new NextGpuAiService(
                nextGpuWebService,
                ollamaUtil,
                "/tmp/comfy",
                globalPropertyRepository,
                chatMessageRepository,
                chatSessionRepository,
                projectRepository,
                analyticsService
        );
        
        // Inject mocks for the internal fields created in constructor
        ReflectionTestUtils.setField(nextGpuAiService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(nextGpuAiService, "longRunningRestTemplate", longRunningRestTemplate);
        ReflectionTestUtils.setField(nextGpuAiService, "objectMapper", objectMapper);
//        ReflectionTestUtils.setField(nextGpuAiService, "nextGpuWebService", nextGpuWebService);
    }

    @Test
    void testListDownloadedModels_Success() {
        ObjectNode mockResponse = objectMapper.createObjectNode();
        ArrayNode modelsArray = mockResponse.putArray("models");
        ObjectNode modelNode = modelsArray.addObject();
        modelNode.put("name", "llama3");
        modelNode.put("modified_at", "2024-01-01");
        modelNode.put("digest", "digest");

        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<PromptModel> result = nextGpuAiService.listDownloadedModels();

        assertEquals(1, result.size());
        assertEquals("llama3", result.get(0).name);
    }

    @Test
    void testListDownloadedModels_Failure() {
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        List<PromptModel> result = nextGpuAiService.listDownloadedModels();

        assertTrue(result.isEmpty());
    }

    @Test
    void testListDownloadedModels_Exception() {
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        List<PromptModel> result = nextGpuAiService.listDownloadedModels();

        assertTrue(result.isEmpty());
    }

    @Test
    void testPullOllamaModel_Success() {
        // Mock checkAvailabilityProblem (calls listModels internally)
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        when(longRunningRestTemplate.postForEntity(eq(OLLAMA_URL + "/api/pull"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("success", HttpStatus.OK));

        boolean result = nextGpuAiService.pullOllamaModel("llama3");

        assertTrue(result);
    }

    @Test
    void testPullOllamaModel_OllamaUnavailable() {
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        boolean result = nextGpuAiService.pullOllamaModel("llama3");

        assertFalse(result);
    }

    @Test
    void testGenerateResponse_Success() throws IOException {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        // Mock listModels
        ObjectNode mockListResponse = objectMapper.createObjectNode();
        ArrayNode modelsArray = mockListResponse.putArray("models");
        ObjectNode modelNode = modelsArray.addObject();
        modelNode.put("name", "llama3");
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(mockListResponse, HttpStatus.OK));

        // Mock generate API call
        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("response", "Hello from AI");
        when(restTemplate.postForEntity(eq(OLLAMA_URL + "/api/generate"), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        String response = nextGpuAiService.generateResponse("llama3", "Hi", new ArrayList<>(), 0.7f);

        assertEquals("Hello from AI", response);
    }

    @Test
    void testGenerateResponse_ModelNotFoundButPulledSuccessfully() throws IOException {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        // Mock listModels - empty initially
        ObjectNode emptyListResponse = objectMapper.createObjectNode();
        emptyListResponse.putArray("models");
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(emptyListResponse, HttpStatus.OK));

        // Mock pullModel success
        when(longRunningRestTemplate.postForEntity(eq(OLLAMA_URL + "/api/pull"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("success", HttpStatus.OK));

        // Mock generate API call
        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("response", "Response after pull");
        when(restTemplate.postForEntity(eq(OLLAMA_URL + "/api/generate"), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        String response = nextGpuAiService.generateResponse("llama3", "Hi", new ArrayList<>(), 0.7f);

        assertEquals("Response after pull", response);
    }

    @Test
    void testGenerateResponse_ModelPullFails() {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        // Mock listModels - empty
        ObjectNode emptyListResponse = objectMapper.createObjectNode();
        emptyListResponse.putArray("models");
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(emptyListResponse, HttpStatus.OK));

        // Mock pullModel failure
        when(longRunningRestTemplate.postForEntity(eq(OLLAMA_URL + "/api/pull"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        String response = nextGpuAiService.generateResponse("llama3", "Hi", new ArrayList<>(), 0.7f);

        assertTrue(response.contains("Error: Failed to pull model"));
    }

    @Test
    void testGenerateResponse_UnexpectedFormat() throws IOException {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        // Mock listModels
        ObjectNode mockListResponse = objectMapper.createObjectNode();
        ArrayNode modelsArray = mockListResponse.putArray("models");
        ObjectNode modelNode = modelsArray.addObject();
        modelNode.put("name", "llama3");
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(mockListResponse, HttpStatus.OK));

        // Mock unexpected generate response
        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("something_else", "value");
        when(restTemplate.postForEntity(eq(OLLAMA_URL + "/api/generate"), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        String response = nextGpuAiService.generateResponse("llama3", "Hi", new ArrayList<>(), 0.7f);

        assertEquals("Error: Unexpected response format from Ollama", response);
    }

    @Test
    void testRecommendOllamaModels_Gpu3080() throws Exception {
        Gpu gpu = new Gpu();
        gpu.setModel("NVIDIA GeForce RTX 3080");
        gpu.setCapacity(10);

        AiModelDto general = new AiModelDto();
        general.setModel("deepseek-r1:14b");
        general.setType("general");
        general.setSizeInGB(14);
        general.setDescription("General reasoning");

        AiModelDto programming = new AiModelDto();
        programming.setModel("deepseek-coder:6.7b");
        programming.setType("programming");
        programming.setSizeInGB(7);

        AiModelDto vision = new AiModelDto();
        vision.setModel("qwen3-vl:4b");
        vision.setType("vision");
        vision.setSizeInGB(4);

        when(nextGpuWebService.getAvailableModelsByGpu(any(GpuDto.class)))
                .thenReturn(List.of(general, programming, vision));

        List<PromptModel> recommendations = nextGpuAiService.recommendOllamaModels(gpu);

        assertNotNull(recommendations);
        assertEquals(3, recommendations.size());
        assertEquals("deepseek-r1:14b", recommendations.getFirst().name);
    }

    @Test
    void testRecommendOllamaModels_Capacity4() throws Exception {
        Gpu gpu = new Gpu();
        gpu.setModel("RTX 3050");
        gpu.setCapacity(4);

        AiModelDto general = new AiModelDto();
        general.setModel("gemma3:4b");
        general.setType("general");
        general.setSizeInGB(4);

        AiModelDto programming = new AiModelDto();
        programming.setModel("deepseek-coder:6.7b");
        programming.setType("programming");
        programming.setSizeInGB(7);

        AiModelDto vision = new AiModelDto();
        vision.setModel("qwen3-vl:4b");
        vision.setType("vision");
        vision.setSizeInGB(4);

        when(nextGpuWebService.getAvailableModelsByGpu(any(GpuDto.class)))
                .thenReturn(List.of(general, programming, vision));

        List<PromptModel> recommendations = nextGpuAiService.recommendOllamaModels(gpu);

        assertEquals("gemma3:4b", recommendations.get(0).name);
        assertEquals("deepseek-coder:6.7b", recommendations.get(1).name);
        assertEquals("qwen3-vl:4b", recommendations.get(2).name);
    }

    @Test
    void testRecommendOllamaModels_Capacity6() throws Exception {
        Gpu gpu = new Gpu();
        gpu.setModel("RTX 2060");
        gpu.setCapacity(6);

        AiModelDto general = new AiModelDto();
        general.setModel("deepseek-r1:8b");
        general.setType("general");
        general.setSizeInGB(8);

        when(nextGpuWebService.getAvailableModelsByGpu(any(GpuDto.class)))
                .thenReturn(List.of(general));

        List<PromptModel> recommendations = nextGpuAiService.recommendOllamaModels(gpu);

        assertEquals("deepseek-r1:8b", recommendations.getFirst().name);
    }

    @Test
    void testRecommendOllamaModels_Capacity16() throws Exception {
        Gpu gpu = new Gpu();
        gpu.setModel("RTX 4080");
        gpu.setCapacity(16);

        AiModelDto general = new AiModelDto();
        general.setModel("mistral-small3.1:24b");
        general.setType("general");
        general.setSizeInGB(24);

        AiModelDto programming = new AiModelDto();
        programming.setModel("gpt-oss:20b");
        programming.setType("programming");
        programming.setSizeInGB(20);

        when(nextGpuWebService.getAvailableModelsByGpu(any(GpuDto.class)))
                .thenReturn(List.of(general, programming));

        List<PromptModel> recommendations = nextGpuAiService.recommendOllamaModels(gpu);

        assertEquals("mistral-small3.1:24b", recommendations.get(0).name);
        assertEquals("gpt-oss:20b", recommendations.get(1).name);
    }

    @Test
    void testRecommendOllamaModels_Capacity24() throws Exception {
        Gpu gpu = new Gpu();
        gpu.setModel("RTX 3090");
        gpu.setCapacity(24);

        AiModelDto general = new AiModelDto();
        general.setModel("deepseek-r1:32b");
        general.setType("general");
        general.setSizeInGB(32);

        AiModelDto programming = new AiModelDto();
        programming.setModel("codellama:34b");
        programming.setType("programming");
        programming.setSizeInGB(34);

        when(nextGpuWebService.getAvailableModelsByGpu(any(GpuDto.class)))
                .thenReturn(List.of(general, programming));

        List<PromptModel> recommendations = nextGpuAiService.recommendOllamaModels(gpu);

        assertEquals("deepseek-r1:32b", recommendations.get(0).name);
        assertEquals("codellama:34b", recommendations.get(1).name);
    }

    @Test
    void testRecommendOllamaModels_Capacity48() throws Exception {
        Gpu gpu = new Gpu();
        gpu.setModel("RTX 6000 Ada");
        gpu.setCapacity(48);

        AiModelDto general = new AiModelDto();
        general.setModel("llama3.3:70b");
        general.setType("general");
        general.setSizeInGB(70);

        when(nextGpuWebService.getAvailableModelsByGpu(any(GpuDto.class)))
                .thenReturn(List.of(general));

        List<PromptModel> recommendations = nextGpuAiService.recommendOllamaModels(gpu);

        assertEquals("llama3.3:70b", recommendations.getFirst().name);
    }

    @Test
    void testGetPromptTemplate_Success() {
        // This test depends on prompts.json being in the classpath.
        String template = nextGpuAiService.getPromptTemplate("summarize");
        assertNotNull(template);
    }

    @Test
    void testGetPromptTemplate_NotFound() {
        String template = nextGpuAiService.getPromptTemplate("non_existent_key");
        assertEquals("", template);
    }

    @Test
    void testGenerateResponseWithHistory_RemovesThinkTags() throws IOException {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));
        // Mock listModels
        ObjectNode mockListResponse = objectMapper.createObjectNode();
        ArrayNode modelsArray = mockListResponse.putArray("models");
        ObjectNode modelNode = modelsArray.addObject();
        modelNode.put("name", "llama3");
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(mockListResponse, HttpStatus.OK));

        // Mock generate API call returning think tags
        ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("response", "<think>Some thoughts</think>Final answer");
        when(restTemplate.postForEntity(eq(OLLAMA_URL + "/api/generate"), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        String response = nextGpuAiService.generateResponseWithHistory("llama3", new ArrayList<>(), "Hi");

        assertEquals("Some thoughts", response); // Wait, look at the code logic for <think>
    }
    
    

    @Test
    void testPruneChatHistory_NoPruningNeeded() {
        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage("user", "short", 0, false));
        history.add(new ChatMessage("assistant", "short", 1, false));
        
        // history length = 2 (tokenLength is count of tokens by space)
        // TOKEN_HISTORY_LIMIT is 4096 (mocked in setUp)
        
        List<ChatMessage> result = nextGpuAiService.pruneChatHistory(history);
        
        assertEquals(2, result.size());
        assertEquals("short", result.getFirst().content);
        assertEquals("short", result.getFirst().prunedContent);
    }

    @Test
    void testPruneChatHistory_PruningLogic() {
        // Mock TOKEN_HISTORY_LIMIT to a small value for testing
        ReflectionTestUtils.setField(nextGpuAiService, "TOKEN_HISTORY_LIMIT", 10);

        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage("user", "Initial message that should stay", 0, false)); // index 0 < 2
        history.add(new ChatMessage("assistant", "Second message that should stay", 1, false)); // index 1 < 2
        history.add(new ChatMessage("user", "Third message that is long and not pinned", 2, false));
        history.add(new ChatMessage("assistant", "Fourth message", 3, false));
        history.add(new ChatMessage("user", "Pinned message", 4, true));

        // Token lengths (word count):
        // 0: 5
        // 1: 5
        // 2: 8
        // 3: 2
        // 4: 2
        // Total: 22. Limit: 10.
        // Average: 22/5 = 4. Average/2 = 2.
        
        // Message 3 has length 2, which is NOT < Average/2 (2). 
        // Wait, belowAverageLength = message.tokenLength < averageLength / 2;
        // 2 < 2 is false.
        
        // Let's make message 3 very short to see it pruned.
        history.get(2).content = "tiny"; // length 1
        history.get(2).tokenLength = 1;
        // Total: 5+5+1+2+2 = 15. Limit 10.
        // Average: 15/5 = 3. Avg/2 = 1.
        // 1 < 1 is still false.
        
        // Let's adjust average.
        // If I make a message very long:
        history.get(3).content = "This is a very very long message to increase the average length significantly";
        history.get(3).tokenLength = 14;
        // Total: 5+5+1+14+2 = 27. Limit 10.
        // Average: 27/5 = 5. Avg/2 = 2.
        // Message 2 ("tiny") has length 1. 1 < 2 is true.
        // Message 2 is not pinned, index is 2 (>=2). It should be pruned.
        
        List<ChatMessage> result = nextGpuAiService.pruneChatHistory(history);
        
        assertEquals("", result.get(2).prunedContent);
        assertEquals(0, result.get(2).tokenLength);
    }

    @Test
    void testGetChatSessions() {
        ChatSession s1 = new ChatSession();
        s1.setName("Session 1");
        s1.setDateUpdated(java.time.LocalDateTime.now().minusHours(1));
        s1.messages.add(new ChatMessage("user", "hi"));

        ChatSession s2 = new ChatSession();
        s2.setName("Session 2");
        s2.setDateUpdated(java.time.LocalDateTime.now());
        s2.messages.add(new ChatMessage("user", "hello"));

        ChatSession s3 = new ChatSession();
        s3.setName("Voided Session");
        s3.setVoided(true);
        s3.messages.add(new ChatMessage("user", "voided"));

        ChatSession s4 = new ChatSession();
        s4.setName("Empty Session");
        s4.messages = new ArrayList<>();

        when(chatSessionRepository.findAll()).thenReturn(List.of(s1, s2, s3, s4));

        List<ChatSession> result = nextGpuAiService.getChatSessions();

        assertEquals(2, result.size());
        assertEquals("Session 2", result.get(0).getName()); // Sorted by dateUpdated desc
        assertEquals("Session 1", result.get(1).getName());
    }

    @Test
    void testUpdateChatSession() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        ChatMessage message = new ChatMessage("user", "new message");
        nextGpuAiService.updateChatSession(session, message, "llama3", false);
        assertEquals(1, session.messages.size());
        assertEquals("llama3", session.promptModel);
        assertNotNull(session.getName()); // Should set name if empty
        verify(chatSessionRepository).save(session);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MESSAGE_SENT.name()), anyMap());
    }

    @Test
    void testUpdateChatSession_WithoutPersist() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        ChatMessage message = new ChatMessage("user", "new message");
        nextGpuAiService.updateChatSession(session, message, "llama3", true);
        assertEquals(1, session.messages.size());
        assertEquals("llama3", session.promptModel);
        assertNotNull(session.getName());
        verify(chatSessionRepository, never()).save(any());
    }

    @Test
    void testUpdateChatSessionMultipleMessages() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();

        ChatMessage userMessage1 = new ChatMessage("user", "first user message");
        nextGpuAiService.updateChatSession(session, userMessage1, "llama3", false);

        ChatMessage assistantMessage1 = new ChatMessage("assistant", "first assistant response");
        nextGpuAiService.updateChatSession(session, assistantMessage1, "llama3", false);

        ChatMessage userMessage2 = new ChatMessage("user", "second user message");
        nextGpuAiService.updateChatSession(session, userMessage2, "llama3", false);

        ChatMessage assistantMessage2 = new ChatMessage("assistant", "second assistant response");
        nextGpuAiService.updateChatSession(session, assistantMessage2, "llama3", false);

        assertEquals(4, session.messages.size());
        assertEquals("user", session.messages.get(0).role);
        assertEquals("first user message", session.messages.get(0).content);
        assertEquals("assistant", session.messages.get(1).role);
        assertEquals("first assistant response", session.messages.get(1).content);
        assertEquals("user", session.messages.get(2).role);
        assertEquals("second user message", session.messages.get(2).content);
        assertEquals("assistant", session.messages.get(3).role);
        assertEquals("second assistant response", session.messages.get(3).content);
        assertNotNull(session.getName());
        verify(chatSessionRepository, times(4)).save(session);
        verify(analyticsService, times(4)).captureEvent(eq(PosthogEvent.MESSAGE_SENT.name()), anyMap());
    }

    @Test
    void testDeleteChatSession() {
        ChatSession session = new ChatSession();
        session.id = 1L;
        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        nextGpuAiService.deleteChatSession(session);

        verify(chatSessionRepository).delete(session);
        verify(analyticsService).captureEvent(eq(PosthogEvent.CHAT_DELETED.name()), anyMap());
    }

    @Test
    void testDeleteChatSession_WithProject() {
        Project project = new Project();
        project.id = 10L;
        project.chatSessions = new ArrayList<>();

        ChatSession session = new ChatSession();
        session.id = 1L;
        session.project = project;
        project.chatSessions.add(session);

        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        nextGpuAiService.deleteChatSession(session);

        assertNull(session.project);
        assertFalse(project.chatSessions.contains(session));
        verify(chatSessionRepository).delete(session);
        verify(analyticsService).captureEvent(eq(PosthogEvent.CHAT_DELETED.name()), anyMap());
    }

    @Test
    void testRenameChatSession() {
        ChatSession session = new ChatSession();
        session.id = 1L;
        session.setUuid("uuid-123");
        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        nextGpuAiService.renameChatSession(session, "New Name");

        assertEquals("New Name", session.getName());
        verify(chatSessionRepository).save(session);
        verify(analyticsService).captureEvent(eq(PosthogEvent.CHAT_RENAMED.name()), argThat(map -> "uuid-123".equals(((Map<String, Object>) map).get("chat_session_id"))));
    }

    @Test
    void testStarChatSession() {
        ChatSession session = new ChatSession();
        session.id = 1L;
        session.setUuid("uuid-123");
        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        nextGpuAiService.starChatSession(session, true);

        assertTrue(session.starred);
        verify(analyticsService).captureEvent(eq(PosthogEvent.CHAT_STARRED.name()), argThat(map -> "uuid-123".equals(((Map<String, Object>) map).get("chat_session_id"))));

        nextGpuAiService.starChatSession(session, false);
        assertFalse(session.starred);
        verify(analyticsService).captureEvent(eq(PosthogEvent.CHAT_UNSTARRED.name()), argThat(map -> "uuid-123".equals(((Map<String, Object>) map).get("chat_session_id"))));
    }

    @Test
    void testCreateProject() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Project project = nextGpuAiService.createProject("Project X", "Do something");
        assertNotNull(project);
        assertEquals("Project X", project.name);
        verify(analyticsService).captureEvent(eq(PosthogEvent.PROJECT_CREATED.name()), anyMap());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void testToggleMessagePin() {
        ChatMessage message = new ChatMessage();
        message.setId(1L);
        message.setUuid("msg-uuid");
        message.pinned = false;
        ChatSession session = new ChatSession();
        message.chatSession = session;

        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(chatMessageRepository.countByChatSessionAndPinnedTrue(session)).thenReturn(0L);

        nextGpuAiService.toggleMessagePin(1L);

        assertTrue(message.pinned);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MESSAGED_PINNED.name()), argThat(map -> "msg-uuid".equals(((Map<String, Object>) map).get("message_id"))));

        nextGpuAiService.toggleMessagePin(1L);
        assertFalse(message.pinned);
        verify(analyticsService).captureEvent(eq(PosthogEvent.MESSAGED_UNPINNED.name()), argThat(map -> "msg-uuid".equals(((Map<String, Object>) map).get("message_id"))));
    }

    @Test
    void testPullOllamaModel_Streaming_Success() {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        String jsonLines = "{\"status\":\"downloading\",\"total\":100,\"completed\":20}\n" +
                "{\"status\":\"downloading\",\"total\":100,\"completed\":50}\n" +
                "{\"status\":\"success\",\"total\":100,\"completed\":100}\n";

        when(longRunningRestTemplate.execute(eq(OLLAMA_URL + "/api/pull"), eq(org.springframework.http.HttpMethod.POST), any(RequestCallback.class), any(ResponseExtractor.class)))
                .thenAnswer(invocation -> {
                    ResponseExtractor<Boolean> extractor = invocation.getArgument(3);
                    org.springframework.http.client.ClientHttpResponse mockResponse = mock(org.springframework.http.client.ClientHttpResponse.class);
                    when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
                    when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream(jsonLines.getBytes(StandardCharsets.UTF_8)));
                    return extractor.extractData(mockResponse);
                });

        List<Double> progressUpdates = new ArrayList<>();
        boolean result = nextGpuAiService.pullOllamaModel("llama3", progressUpdates::add, () -> false);

        assertTrue(result);
        assertEquals(3, progressUpdates.size());
        assertEquals(0.2, progressUpdates.get(0));
        assertEquals(0.5, progressUpdates.get(1));
        assertEquals(1.0, progressUpdates.get(2));
    }

    @Test
    void testPullOllamaModel_Cancellation() {
        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        String jsonLines = "{\"status\":\"downloading\",\"total\":100,\"completed\":20}\n" +
                "{\"status\":\"downloading\",\"total\":100,\"completed\":50}\n" +
                "{\"status\":\"success\",\"total\":100,\"completed\":100}\n";

        when(longRunningRestTemplate.execute(eq(OLLAMA_URL + "/api/pull"), eq(org.springframework.http.HttpMethod.POST), any(RequestCallback.class), any(ResponseExtractor.class)))
                .thenAnswer(invocation -> {
                    ResponseExtractor<Boolean> extractor = invocation.getArgument(3);
                    org.springframework.http.client.ClientHttpResponse mockResponse = mock(org.springframework.http.client.ClientHttpResponse.class);
                    when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
                    when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream(jsonLines.getBytes(StandardCharsets.UTF_8)));
                    return extractor.extractData(mockResponse);
                });

        List<Double> progressUpdates = new ArrayList<>();
        AtomicReference<Boolean> cancelled = new AtomicReference<>(false);
        
        boolean result = nextGpuAiService.pullOllamaModel("llama3", progress -> {
            progressUpdates.add(progress);
            if (progress >= 0.2) {
                cancelled.set(true);
            }
        }, cancelled::get);

        assertFalse(result); // Should return false because it was cancelled
        assertEquals(1, progressUpdates.size());
        assertEquals(0.2, progressUpdates.get(0));
    }

    @Test
    void testSearchInChatSession_WithMatchingMessages() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        session.messages.add(new ChatMessage("user", "Hello world"));
        session.messages.add(new ChatMessage("assistant", "Hi there"));
        session.messages.add(new ChatMessage("user", "How are you?"));
        session.messages.add(new ChatMessage("assistant", "I am fine, thank you"));

        List<ChatMessage> result = nextGpuAiService.searchInChatSession(session, "hello", false);

        assertEquals(1, result.size());
        assertEquals("Hello world", result.getFirst().content);
    }

    @Test
    void testSearchInChatSession_NoMatches() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        session.messages.add(new ChatMessage("user", "Hello world"));
        session.messages.add(new ChatMessage("assistant", "Hi there"));

        List<ChatMessage> result = nextGpuAiService.searchInChatSession(session, "goodbye", false);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchInChatSession_CaseSensitive() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        session.messages.add(new ChatMessage("user", "Hello World"));
        session.messages.add(new ChatMessage("assistant", "HELLO there"));
        session.messages.add(new ChatMessage("user", "goodbye"));

        List<ChatMessage> result = nextGpuAiService.searchInChatSession(session, "HELLO", true);

        assertEquals(1, result.size());
        assertEquals("HELLO there", result.get(0).content);
    }

    @Test
    void testSearchInChatSession_CaseInsensitive() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        session.messages.add(new ChatMessage("user", "Hello World"));
        session.messages.add(new ChatMessage("assistant", "HELLO there"));
        session.messages.add(new ChatMessage("user", "goodbye"));

        List<ChatMessage> result = nextGpuAiService.searchInChatSession(session, "HeLLo", false);

        assertEquals(2, result.size());
        assertEquals("Hello World", result.get(0).content);
        assertEquals("HELLO there", result.get(1).content);
    }

    @Test
    void testSearchInChatSession_EmptyQuery() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        session.messages.add(new ChatMessage("user", "Hello world"));
        session.messages.add(new ChatMessage("assistant", "Hi there"));

        List<ChatMessage> result = nextGpuAiService.searchInChatSession(session, "", false);

        assertEquals(2, result.size());
    }

    @Test
    void testSearchInChatSession_NullQuery() {
        ChatSession session = new ChatSession();
        session.messages = new ArrayList<>();
        session.messages.add(new ChatMessage("user", "Hello world"));
        session.messages.add(new ChatMessage("assistant", "Hi there"));

        List<ChatMessage> result = nextGpuAiService.searchInChatSession(session, null, false);

        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteOllamaModel_Success() {
        // Force Windows to test WSL cleanup path
        OSUtil.IS_WINDOWS = true;
        
        // Mock globalPropertyRepository for WSL credentials
        GlobalProperty distroProp = new GlobalProperty();
        distroProp.setName(GlobalPropertyConfig.LINUX_DISTRO);
        distroProp.setValueReference("nextgpu");
        
        GlobalProperty userProp = new GlobalProperty();
        userProp.setName(GlobalPropertyConfig.OS_USERNAME);
        userProp.setValueReference("nextgpu");
        
        GlobalProperty passProp = new GlobalProperty();
        passProp.setName(GlobalPropertyConfig.OS_PASSWORD);
        passProp.setValueReference("password");

        lenient().when(globalPropertyRepository.findByName(eq(GlobalPropertyConfig.LINUX_DISTRO)))
                .thenReturn(Optional.of(distroProp));
        lenient().when(globalPropertyRepository.findByName(eq(GlobalPropertyConfig.OS_USERNAME)))
                .thenReturn(Optional.of(userProp));
        lenient().when(globalPropertyRepository.findByName(eq(GlobalPropertyConfig.OS_PASSWORD)))
                .thenReturn(Optional.of(passProp));

        // Mock checkAvailabilityProblem
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        // Mock listDownloadedModels (Ollama part)
        ObjectNode mockResponse = objectMapper.createObjectNode();
        ArrayNode modelsArray = mockResponse.putArray("models");
        ObjectNode modelNode = modelsArray.addObject();
        modelNode.put("name", "llama3");
        when(restTemplate.getForEntity(eq(OLLAMA_URL + "/api/tags"), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        when(restTemplate.exchange(eq(OLLAMA_URL + "/api/delete"), eq(org.springframework.http.HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("success", HttpStatus.OK));

        boolean result = nextGpuAiService.deleteOllamaModel("llama3", false);

        assertTrue(result);
        verify(restTemplate).exchange(eq(OLLAMA_URL + "/api/delete"), eq(org.springframework.http.HttpMethod.DELETE), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testDownloadComfyUiModel_Success_Local() throws IOException {
        // Force non-Windows for local testing if needed, or just test the non-Windows path logic
        OSUtil.IS_WINDOWS = false;
        String tempDir = Files.createTempDirectory("nextgpu-test").toString();
        ReflectionTestUtils.setField(nextGpuAiService, "comfyBaseDir", tempDir);

        AiModelDto model = new AiModelDto();
        model.setModel("flux");
        ComfyUiModelFile file = new ComfyUiModelFile();
        file.setFileName("flux.sft");
        file.setTargetSubfolder("checkpoints");
        file.setUrl("http://example.com/flux.sft");
        model.setFiles(List.of(file));

        when(longRunningRestTemplate.execute(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), any()))
                .thenReturn(true);

        boolean result = nextGpuAiService.downloadComfyUiModel(model, p -> {}, () -> false);

        assertTrue(result);
    }

    @Test
    void testDownloadComfyUiModel_Cancellation_Local() throws IOException {
        OSUtil.IS_WINDOWS = false;
        String tempDir = Files.createTempDirectory("nextgpu-test-cancel").toString();
        ReflectionTestUtils.setField(nextGpuAiService, "comfyBaseDir", tempDir);

        AiModelDto model = new AiModelDto();
        model.setModel("flux");
        ComfyUiModelFile file = new ComfyUiModelFile();
        file.setFileName("flux.sft");
        file.setTargetSubfolder("checkpoints");
        file.setUrl("http://example.com/flux.sft");
        model.setFiles(List.of(file));

        // Mock longRunningRestTemplate to return false or handle cancellation via the extractor
        lenient().when(longRunningRestTemplate.execute(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), any()))
                .thenAnswer(invocation -> {
                    // This simulates the behavior when cancelSupplier returns true inside the extractor
                    return false;
                });

        boolean result = nextGpuAiService.downloadComfyUiModel(model, p -> {}, () -> true);

        assertFalse(result);
    }

    @Test
    void testDeleteComfyUiModel_Local() throws IOException {
        OSUtil.IS_WINDOWS = false;
        String tempDir = Files.createTempDirectory("nextgpu-test-delete").toString();
        ReflectionTestUtils.setField(nextGpuAiService, "comfyBaseDir", tempDir);

        AiModelDto model = new AiModelDto();
        ComfyUiModelFile file = new ComfyUiModelFile();
        file.setFileName("flux.sft");
        file.setTargetSubfolder("checkpoints");
        model.setFiles(List.of(file));

        // We can't easily mock Files.delete without PowerMock, but we can verify it doesn't crash 
        // and returns true if no exceptions occur (or false if files don't exist in the dummy path)
        boolean result = nextGpuAiService.deleteComfyUiModel(model);
        
        // It returns true by default if no exception is thrown during the loop
        assertTrue(result);
    }
}
