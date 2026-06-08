package ai.nextgpu.agent

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.service.AnalyticsService
import ai.nextgpu.common.model.PosthogEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Service
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets


@Service
open class OllamaStreamingService {

    @Autowired
    private lateinit var analyticsService: AnalyticsService

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Value("\${ollama.api.url:http://127.0.0.1:11434}")
    private lateinit var ollamaBaseUrl: String

    /**
     * Sends a prompt to Ollama and emits partial responses as they are generated.
     */
    fun streamGenerate(
        model: String,
        prompt: String,
        baseUrl: String? = null
    ): Flow<String> = callbackFlow {

        val finalBaseUrl = baseUrl ?: ollamaBaseUrl

        val body = mapOf(
            "model" to model,
            "prompt" to prompt,
            "stream" to true
        )

        val payload = objectMapper.writeValueAsString(body)

        // Prepare callbacks for streaming response
        val requestCallback = RequestCallback { request ->
            request.headers.contentType = MediaType.APPLICATION_JSON
            request.body.write(payload.toByteArray(StandardCharsets.UTF_8))
        }

        val responseExtractor = ResponseExtractor { response: ClientHttpResponse ->
            if (!response.statusCode.is2xxSuccessful) {
                close(RuntimeException("Ollama error ${response.statusCode.value()}"))
                return@ResponseExtractor null
            }

            response.body.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    var line: String?
                    while (true) {
                        line = reader.readLine() ?: break
                        if (line.isBlank()) continue

                        val node: JsonNode = objectMapper.readTree(line)
                        if (node.path("done").asBoolean(false)) break

                        val chunk = node.path("response").asText(null)
                        if (!chunk.isNullOrEmpty()) {
                            // Send each chunk to the Flow collector
                            trySend(chunk)
                        }
                    }
                }
            }
            // Signal normal completion
            close()
            null
        }

        // Execute the request and stream the response
        restTemplate.execute(
            "$finalBaseUrl/api/generate",
            POST,
            requestCallback,
            responseExtractor
        )
        awaitClose { }
    }

    /**
     * Sends a conversation history to Ollama and emits partial responses as they are generated.
     */
    fun streamChat(
        model: String,
        messages: List<ChatMessage>,
        baseUrl: String? = null
    ): Flow<String> = callbackFlow {

        val finalBaseUrl = baseUrl ?: ollamaBaseUrl

        val body = mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.prunedContent) },
            "stream" to true
        )

        val payload = objectMapper.writeValueAsString(body)

        // Prepare callbacks for streaming response
        val requestCallback = RequestCallback { request ->
            request.headers.contentType = MediaType.APPLICATION_JSON
            request.body.write(payload.toByteArray(StandardCharsets.UTF_8))
        }

        // Get the payload size by calling the ObjectSizeFetcher
        var packetSize = 0

        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject(body)
                packetSize = bos.toByteArray().size
            }
        }

        val queryReceivedTimestamp = System.currentTimeMillis()
        var llmUnderstoodQueryInMS = 0L // In milliseconds
        var responseGeneratedInMS = 0L // In milliseconds

        val responseExtractor = ResponseExtractor { response: ClientHttpResponse ->
            if (!response.statusCode.is2xxSuccessful) {
                close(RuntimeException("Ollama error ${response.statusCode.value()}"))
                return@ResponseExtractor null
            }

            var responseStarted = false
            response.body.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                    var line: String?
                    while (true) {
                        line = reader.readLine() ?: break
                        if (line.isBlank()) continue

                        val node: JsonNode = objectMapper.readTree(line)
                        if (node.path("done").asBoolean(false)) break

                        val chunk = node.path("message")?.path("content")?.asText(null)
                        if (!chunk.isNullOrEmpty()) {
                            // Send each chunk to the Flow collector
                            if (!responseStarted) {
                                responseStarted = true
                                // Get the timestamp when llm understood the query
                                llmUnderstoodQueryInMS = System.currentTimeMillis() - queryReceivedTimestamp;
                            }
                            trySend(chunk)
                        }
                    }
                }
            }
            // Get the response generated timestamp
            responseGeneratedInMS = System.currentTimeMillis() - llmUnderstoodQueryInMS

            // Notify LLM Response time w.r.t model
            val eventData = mutableMapOf<String, Any>(
                "model_name" to model,
                "packet_size" to packetSize,
//                "query_received_ms" to queryReceivedTimestamp,
                "llm_understood_query_ms" to llmUnderstoodQueryInMS,
                "response_generated_ms" to responseGeneratedInMS,
            )

            // Notify LLM Response time w.r.t model
            analyticsService.captureEvent(PosthogEvent.LLM_QUERY_RESPONSE_TIME.name, eventData)

            // Signal normal completion
            close()
            null
        }
        // Execute the request and stream the response
        restTemplate.execute(
            "$finalBaseUrl/api/chat",
            org.springframework.http.HttpMethod.POST,
            requestCallback,
            responseExtractor
        )
        awaitClose { }
    }
}
