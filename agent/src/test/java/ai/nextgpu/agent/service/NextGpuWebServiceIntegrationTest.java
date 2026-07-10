package ai.nextgpu.agent.service;

import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.SttDto;
import ai.nextgpu.common.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.nio.file.Paths;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NextGpuWebServiceIntegrationTest {

    private final GlobalPropertyRepository globalPropertyRepository = mock(GlobalPropertyRepository.class);
    private final HttpUtil httpUtil = new HttpUtil(globalPropertyRepository);
    private final NextGpuWebService service = new NextGpuWebService(httpUtil);

    @BeforeAll
    void verifyLocalSttServiceIsRunning() {
        assumeTrue(service.isSttServiceAvailable(),
                "Local STT service is not running on http://localhost:8110");
    }

    @Test
    void getTransformedString_transformsHarvardAudioFile_usingLocalSttService() throws Exception {
        File audioFile = getExampleAudioFile("harvard.wav");

        SttDto result = service.getTransformedString(audioFile);
        System.out.println("Harvard transcription result: " + result);

        assertValidTranscription(result.getText());
        assertTrue(
                normalized(result.getText()).contains("stale smell")
                        || normalized(result.getText()).contains("old beer")
                        || normalized(result.getText()).contains("takes heat"),
                "Unexpected Harvard transcription: " + result
        );
    }

    @Test
    void getTransformedString_transformsJackhammerAudioFile_usingLocalSttService() throws Exception {
        File audioFile = getExampleAudioFile("jackhammer.wav");

        SttDto result = service.getTransformedString(audioFile);
        System.out.println("Jackhammer transcription result: " + result);

        assertNotNull(result);
        assertValidTranscription(result.getText());
        assertTrue(
                normalized(result.getText()).contains("stale smell")
                        || normalized(result.getText()).contains("lingers"),
                "Unexpected Harvard transcription: " + result
        );
        assertFalse(
                normalized(result.getText()).contains("error"),
                "Transcription response should not contain an error message: " + result
        );
    }

    private File getExampleAudioFile(String fileName) {
        File audioFile = Paths.get("src", "test", "java", "examples", fileName).toFile();

        assertTrue(audioFile.exists(), "Expected example audio file to exist: " + audioFile.getAbsolutePath());
        assertTrue(audioFile.isFile(), "Expected example audio path to be a file: " + audioFile.getAbsolutePath());

        return audioFile;
    }

    private void assertValidTranscription(String text) {
        assertFalse(text == null || text.isBlank(), "Transcription response should not be blank");
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}