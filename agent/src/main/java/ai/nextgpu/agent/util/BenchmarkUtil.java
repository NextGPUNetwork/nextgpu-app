package ai.nextgpu.agent.util;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.service.NextGpuAgentService;
import ai.nextgpu.agent.service.NextGpuAiService;
import ai.nextgpu.common.model.GlobalProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for benchmarking the CPU's single-core performance.
 * Provides methods to compute benchmark scores by executing specific tasks
 * under controlled conditions.
 *
 * This class uses a single-threaded executor for benchmarking tasks to ensure
 * consistency and avoid bottlenecks caused by multithreading.
 *
 * The benchmark operates by running intensive computations such as prime number
 * checks to evaluate CPU capability. This computation is encapsulated in a task
 * executed asynchronously and with timeout handling.
 */
@Component
public class BenchmarkUtil {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkUtil.class);

    @Autowired
    NextGpuAgentService agentService;

    @Autowired
    NextGpuAiService aiService;

    // Very important to limit the benchmarking to single thread to avoid bottlenecking
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Runs benchmark tasks safely
    private Integer runBenchmarkTask(Callable<Integer> task, String benchmarkName) {
        Future<Integer> future = executorService.submit(task);
        try {
            return future.get(2, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Benchmark {} failed: {}", benchmarkName, e.getMessage());
            return -1;
        }
    }

    /**
     * Runs single and multi-core CPU benchmarks using sysbench and returns a combined result.
     *
     * @return a map containing results for both singlecore and multicore performance.
     */
    public Map<String, Object> benchmarkCpu() {
        Map<String, Object> combinedResults = new HashMap<>();

        Map<String, Object> cpuSingleCore = benchmarkCpu(1);
        cpuSingleCore.forEach((key, value) -> combinedResults.put("singlecore_" + key, value));

        int numCores = Runtime.getRuntime().availableProcessors();
        Map<String, Object> cpuMultiCore = benchmarkCpu(numCores);
        cpuMultiCore.forEach((key, value) -> combinedResults.put("multicore_" + key, value));

        return combinedResults;
    }

    /**
     * Runs a benchmark to evaluate the CPU's single-core performance using sysbench.
     * The benchmark executes a CPU-intensive prime number calculation with a single thread.
     * Sysbench is expected to be pre-installed on the operating system.
     *
     * @return a map containing the benchmark result where the key represents the performance metric
     *         (e.g., "events_per_second") and the value represents the corresponding score as an integer.
     */
    private Map<String, Object> benchmarkCpuSingleCore() {
        return benchmarkCpu(1);
    }

    /**
     * Runs a benchmark to evaluate the CPU's multi-core performance.
     * The benchmark utilizes all available CPU cores and measures performance
     * in a multi-threaded environment. This method retrieves the number of
     * available cores and delegates the benchmarking process to the underlying
     * multi-core benchmarking method.
     *
     * @return a map containing the benchmark results with keys representing
     *         performance metrics (e.g., "events_per_second") and values representing
     *         the corresponding scores as integers.
     */
    private Map<String, Object> benchmarkCpuMultiCore() {
        // Get the number of available CPU cores
        int numCores = Runtime.getRuntime().availableProcessors();
        return benchmarkCpu(numCores);
    }

    /**
     * Runs a CPU benchmark using the sysbench tool with the specified number of threads.
     * The benchmark measures multi-threaded CPU performance and extracts performance metrics
     * from the sysbench output by calculating 100000 prime numbers 100000 times.
     *
     * @param threads the number of threads to use for the benchmark
     * @return a map containing the benchmark results where the keys represent the performance metrics
     *         (e.g., "events_per_second") and the values represent the corresponding scores as integers;
     *         an empty map is returned if the benchmark fails or encounters an error
     */
    private Map<String, Object> benchmarkCpu(int threads) {
        Map<String, Object> scores = new HashMap<>();
        try {
            // Get the username and distro for WSL
            GlobalProperty usernameProp = agentService.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME);
            GlobalProperty distroProp = agentService.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO);

            if (usernameProp == null || distroProp == null) {
                log.error("OS username or Linux distro property not found for CPU benchmark");
                return scores;
            }

            String username = usernameProp.getValueReference();
            String distro = distroProp.getValueReference();

            // Start the benchmark timer
            long start = System.nanoTime();

            // Run sysbench CPU test with multiple threads (one per core)
            String command = "sysbench cpu --cpu-max-prime=100000 --max-requests=100000 --threads=" + threads + " run";
            String result = OSUtil.executeCommandInWsl(command, distro, username);

            long end = System.nanoTime() - start;

            // Parse the result to extract the events per second
            scores = parseSysbenchResult(result);
            if (!scores.isEmpty()) {
                scores.put("total_runtime_ms", (int) (end / 1_000_000));
            }
            log.info("Sysbench output: {}", result);
        } catch (Exception e) {
            log.error("Error running CPU benchmark: {}", e.getMessage());
        }
        return scores;
    }

    /**
     * Parses the output of sysbench to extract the events per second metric.
     *
     * @param rawOutput the output string from sysbench
     * @return the events per second as an integer
     */
    private Map<String, Object> parseSysbenchResult(String rawOutput) {
        Map<String, Object> scores = new HashMap<>();
        if (rawOutput == null || rawOutput.isEmpty() || rawOutput.contains("Wsl/Service/WSL_E_DISTRO_NOT_FOUND")) {
            log.error("Sysbench output is empty or WSL distro not found");
            return scores;
        }
        // Look for the line containing "events per second"
        for (String line : rawOutput.split("\\n")) {
            try {
                String[] lookfor = {"events per second", "total number of events"};
                if (Arrays.stream(lookfor).anyMatch(line::contains)) {
                    String[] parts = line.trim().split(":");
                    if (parts.length >= 2) {
                        double value = Double.parseDouble(parts[1].trim());
                        scores.put(parts[0].replace(" ", "_"), (int) value);
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                log.error("Failed to parse events per second: {}", e.getMessage());
            }
        }
        return scores;
    }

    /** Model used for the GPU benchmark's inference workload. */
    private static final String GPU_BENCHMARK_MODEL = "deepseek-r1:1.5b";

    /** Prompt engineered to require at least 1,000 words of generation to produce a meaningful throughput sample. */
    private static final String GPU_BENCHMARK_PROMPT = """
            Write technical explanation of how modern GPUs accelerate large language model inference.
            Structure the response in the following sections:
            1. Matrix multiplication and tensor cores
            2. Attention mechanisms
            3. Memory bandwidth and VRAM constraints
            4. Quantization methods such as INT8 and 4-bit
            5. Parallelism strategies used in inference engines
            6. Performance bottlenecks in consumer GPUs
            Each section must contain at least three paragraphs.
            Write in clear technical prose and avoid bullet points.
            The total response should be at least 1000 words.
        """;

    /** Small, cheap prompt used only to force the model to load into VRAM before the timed run. */
    private static final String GPU_WARMUP_PROMPT = "Reply with the single word OK.";

    private static final int GPU_BENCHMARK_MAX_ATTEMPTS = 4;
    private static final long GPU_BENCHMARK_INITIAL_BACKOFF_MS = 3_000;
    private static final long GPU_BENCHMARK_MAX_BACKOFF_MS = 30_000;

    /**
     * Executes a GPU benchmark by instructing a local or remote AI service to generate a comprehensive
     * technical document, measuring the generation throughput in tokens per second.
     * <p>
     * The benchmark uses a complex prompt (requiring at least 1,000 words on GPU architecture and LLM inference)
     * targeting the {@link #GPU_BENCHMARK_MODEL} model to simulate a demanding workload. It parses performance
     * metrics directly from the AI service's metadata payload.
     * </p>
     * <p>
     * This method is resilient to a cold or recovering Ollama backend (e.g. right after the WSL service was
     * restarted/force-started by {@code OllamaUtil}'s self-recovery, or during first-time model loading into
     * VRAM):
     * </p>
     * <ul>
     *     <li>a cheap, discarded warm-up call is made first so that model-load time is never counted as part of
     *     the timed throughput sample, and so it also serves as an early-out connectivity probe;</li>
     *     <li>on failure (of either the warm-up or the timed call), the attempt is retried with exponential
     *     backoff up to {@link #GPU_BENCHMARK_MAX_ATTEMPTS} times, since Ollama recovery/model-loading in WSL can
     *     legitimately take longer than a single immediate retry allows for;</li>
     *     <li>the underlying failure reason (connectivity error, malformed payload, etc.) is preserved and
     *     surfaced rather than collapsed into a generic message.</li>
     * </ul>
     *
     * @return a {@link Map} containing the benchmark results under the key {@code "gpu0"}.
     *         The nested metrics map includes:
     *         <ul>
     *           <li>{@code "tokens"} (Integer): The total number of tokens evaluated/generated.</li>
     *           <li>{@code "elapsedTime"} (Integer): The processing duration in nanoseconds.</li>
     *           <li>{@code "tokens_per_second"} (Double): The calculated throughput performance metric.</li>
     *         </ul>
     * @throws RuntimeException if every attempt fails; the cause chain preserves the last underlying failure.
     */
    public Map<String, Object> benchmarkGpu() {
        Map<String, Object> allResults = new HashMap<>();
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= GPU_BENCHMARK_MAX_ATTEMPTS; attempt++) {
            try {
                // Discard the result: this just forces the model into VRAM and confirms the service is actually
                // responsive before we start the clock. Without this, the *first* successful attempt after a
                // cold start/recovery would report a misleadingly low tokens_per_second dominated by model load
                // time rather than steady-state generation speed.
                runGpuInference(GPU_WARMUP_PROMPT);

                Map<String, Object> scores = runGpuInferenceTimed(GPU_BENCHMARK_PROMPT);
                allResults.put("gpu0", scores);
                if (attempt > 1) {
                    log.info("GPU benchmark succeeded on attempt {}/{}.", attempt, GPU_BENCHMARK_MAX_ATTEMPTS);
                }
                return allResults;
            } catch (Exception e) {
                lastFailure = e;
                boolean hasNext = attempt < GPU_BENCHMARK_MAX_ATTEMPTS;
                log.warn("GPU benchmark attempt {}/{} failed: {}.{}", attempt, GPU_BENCHMARK_MAX_ATTEMPTS,
                        e.getMessage(), hasNext ? " Retrying." : "");
                if (hasNext) {
                    sleepQuietly(backoffMs(attempt));
                }
            }
        }

        log.error("GPU benchmark failed after {} attempts: {}", GPU_BENCHMARK_MAX_ATTEMPTS,
                lastFailure.getMessage());
        throw new RuntimeException("Failed to run GPU benchmark after " + GPU_BENCHMARK_MAX_ATTEMPTS + " attempts", lastFailure);
    }

    /**
     * Runs the timed GPU inference call and extracts throughput metrics from the response.
     *
     * @param prompt the prompt to send
     * @return a map with {@code tokens}, {@code elapsedTime} (ns), and {@code tokens_per_second}
     * @throws RuntimeException if the AI service reports an error or returns a payload missing eval metadata
     */
    private Map<String, Object> runGpuInferenceTimed(String prompt) {
        Map<String, Object> scores = new HashMap<>();
        JsonNode response = runGpuInference(prompt);

        int tokens = response.get("eval_count").asInt();
        int elapsedTime = response.get("eval_duration").asInt();    // In nanoseconds
        double tokensPerSecond = (tokens * 1_000_000_000.0) / (elapsedTime + 1);

        scores.put("tokens", tokens);
        scores.put("elapsedTime", elapsedTime);
        scores.put("tokens_per_second", tokensPerSecond);
        return scores;
    }

    /**
     * Calls the AI service for the GPU benchmark's model and validates the response, surfacing the real failure
     * reason instead of a generic message. {@code generateResponseRaw} does not throw on connectivity failures;
     * it returns an {@code {"error": ...}} payload instead, so that case is detected explicitly here.
     *
     * @param prompt the prompt to send
     * @return the raw JSON response, guaranteed non-null, error-free, and containing eval metadata
     * @throws RuntimeException if the response is null, carries an "error" field, or is missing eval metadata
     */
    private JsonNode runGpuInference(String prompt) {
        JsonNode response = aiService.generateResponseRaw(GPU_BENCHMARK_MODEL, prompt, new ArrayList<>(), 0.0f);
        if (response == null) {
            throw new RuntimeException("No response from AI service for GPU benchmark");
        }
        if (response.has("error")) {
            throw new RuntimeException("AI service error during GPU benchmark: " + response.get("error").asText());
        }
        if (!response.has("eval_count") || !response.has("eval_duration")) {
            throw new RuntimeException("AI service response missing eval metadata for GPU benchmark: " + response);
        }
        return response;
    }

    private long backoffMs(int attempt) {
        long backoff = GPU_BENCHMARK_INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        return Math.min(backoff, GPU_BENCHMARK_MAX_BACKOFF_MS);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Double> parseGlMark2Result(String rawOutput) {
        Map<String, List<Double>> tempResults = new HashMap<>();
        Map<String, Double> result = new HashMap<>();

        double surfaceWidth = 1280;
        double surfaceHeight = 720;

        // Regex patterns for extracting relevant data
        Pattern surfacePattern = Pattern.compile("Surface Size:\\s*(\\d+)x(\\d+)");
        Pattern categoryPattern = Pattern.compile("^\\[(\\w+)\\].*?FPS:\\s*([\\d.]+)");

        String[] lines = rawOutput.split("\n");
        for (String line : lines) {
            line = line.trim();

            // Surface size (window width and height)
            Matcher surfMatcher = surfacePattern.matcher(line);
            if (surfMatcher.find()) {
                surfaceWidth = Double.parseDouble(surfMatcher.group(1));
                surfaceHeight = Double.parseDouble(surfMatcher.group(2));
                continue;
            }

            // Extract FPS for each category
            Matcher catMatcher = categoryPattern.matcher(line);
            if (catMatcher.find()) {
                String category = catMatcher.group(1).toLowerCase();
                double fps = Double.parseDouble(catMatcher.group(2));
                tempResults.computeIfAbsent(category, k -> new ArrayList<>()).add(fps);
            }
        }
        // Average the FPS for categories with multiple entries
        List<String> categories = Arrays.asList("build", "texture", "shading", "bump", "effect2d", "pulsar", "desktop",
                "buffer", "ideas", "jellyfish", "shadow", "refract", "conditionals", "function", "loop");
        for (String category : categories) {
            List<Double> values = tempResults.get(category);
            if (values != null && !values.isEmpty()) {
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                result.put(category, avg);
            }
        }

        // Calculate grand average FPS of all categories
        double glscore = result.keySet().stream()   // For every result object
                .filter(key -> categories.contains(key))    // Restrict to listed categories only
                .mapToDouble(key -> result.get(key))    // Fetch the FPS value
                .average().orElse(0);   // Calculate average
        result.put("glscore", glscore);

        // Add surface width/height
        if (surfaceWidth > 0) result.put("surface_width", surfaceWidth);
        if (surfaceHeight > 0) result.put("surface_height", surfaceHeight);

        return result;
    }

    /**
     * Runs a benchmark to evaluate memory performance by measuring write speed,
     * read speed, and memory access latency over five runs. The method calculates
     * the average results across these runs and returns them as a map.
     *
     * @return a map containing the average memory benchmark results, with the keys
     *         "write", "read", and "latency" representing write speed (MB/s),
     *         read speed (MB/s), and memory latency (ns) respectively.
     */
    public Map<String, Object> benchmarkMemory(boolean quickBenchmark) {
        List<Map<String, Object>> allResults = new ArrayList<>();

        // TODO memoryBenchmarkTool ??????????
        for (int run = 0; run < (quickBenchmark ? 2 : 5); run++) {
            Map<String, Object> scores = new HashMap<>();

            // Use a smaller buffer size for testing
            int BUFFER_SIZE = 256 * 1024 * 1024; // 100MB buffer size
            byte[] buffer = new byte[BUFFER_SIZE];

            // Detect write speed
            long start = System.nanoTime();
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (byte) (i % 256);
            }
            long durationNs = System.nanoTime() - start;
            double throughputMBps = BUFFER_SIZE / (durationNs / 1e9) / (1024 * 1024);
            log.info("Run {} - Write Speed: {} MB/s", run + 1, String.format("%.2f", throughputMBps));
            scores.put("write", (int) throughputMBps);

            // Detect read speed
            long sum = 0;
            start = System.nanoTime();
            for (int i = 0; i < buffer.length; i++) {
                sum += buffer[i];
            }
            durationNs = System.nanoTime() - start;
            throughputMBps = BUFFER_SIZE / (durationNs / 1e9) / (1024 * 1024);
            log.info("Run {} - Read Speed: {} MB/s | Checksum: {}", run + 1, String.format("%.2f", throughputMBps), sum);
            scores.put("read", (int) throughputMBps);

            // Detect memory latency
            start = System.nanoTime();
            int steps = 10_000_000;
            int index = 0;
            for (int i = 0; i < steps; i++) {
                index = (index + buffer[index % buffer.length]) % buffer.length;
            }
            durationNs = System.nanoTime() - start;
            double avgLatencyNs = (double) durationNs / steps;
            log.info("Run {} - Approx. Memory Latency: {} ns per access", run + 1, String.format("%.2f", avgLatencyNs));
            scores.put("latency", (int) avgLatencyNs);

            allResults.add(scores);
        }

        // Calculate average results directly
        Map<String, Object> averageScores = new HashMap<>();

        for (String key : allResults.get(0).keySet()) {
            int sum = allResults.stream()
                    .mapToInt(map -> (int) map.get(key))
                    .sum();
            averageScores.put(key, sum / allResults.size());
        }
        return averageScores;
    }

    /**
     * Executes a storage benchmark to measure read and write performance, latency,
     * and disk utilization. The benchmarking process uses the "fio" tool and
     * parses the generated JSON output to extract the relevant metrics.
     *
     * @param quickBenchmark if true, performs a quicker benchmark with smaller
     *                        file size and fewer iterations; otherwise, performs
     *                        a more comprehensive benchmark.
     * @return a map containing the benchmark results with the following keys:
     *         "read_bandwidth_mb" - read bandwidth in MB/s,
     *         "read_iops" - read input/output operations per second (IOPS),
     *         "read_latency_ns" - average read latency in nanoseconds,
     *         "write_bandwidth_mb" - write bandwidth in MB/s,
     *         "write_iops" - write IOPS,
     *         "write_latency_ns" - average write latency in nanoseconds,
     *         "total_runtime_ms" - running time of the benchmark test
     */
    public Map<String, Object> benchmarkStorage(boolean quickBenchmark) {
        String fioCommand = "fio " +
                "--name=storagetest " +
                "--filename=deleteme.tmp " +
                (quickBenchmark ? "--size=256M " : "--size=1G ") +
                "--bs=4k " +
                "--rw=randrw " +
                "--numjobs=4 " +
                (quickBenchmark ? "--loops=1 " : "--loops=2 ") +
                "--group_reporting " +
                "--output-format=json";
        Map<String, Object> scores = new HashMap<>();
        try {
            long start = System.nanoTime();
            // Execute the FIO command in WSL
            GlobalProperty usernameProp = agentService.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME);
            GlobalProperty distroProp = agentService.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO);

            if (usernameProp == null || distroProp == null) {
                log.error("OS username or Linux distro property not found");
                throw new RuntimeException("Storage benchmark failed");
            }

            String username = usernameProp.getValueReference();
            String distro = distroProp.getValueReference();
            String result = OSUtil.executeCommandInWsl(fioCommand, distro, username);
            log.info("FIO result: {}", result);

            if (result.isEmpty() || result.contains("Wsl/Service/WSL_E_DISTRO_NOT_FOUND")) {
                log.error("FIO output is empty or WSL distro not found");
                throw new RuntimeException("Storage benchmark failed");
            }

            // Parse the JSON output
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(result);

            // Extract the benchmark metrics from the Jobs array
            JsonNode jobsNode = rootNode.path("jobs").get(0);

            // Extract read metrics
            JsonNode readNode = jobsNode.path("read");
            int readBandwidth = readNode.path("bw").asInt();
            int readIOPS = (int) readNode.path("iops").asDouble();
            JsonNode readLatencyNode = jobsNode.path("clat_ns");
            double readLatency = readLatencyNode.path("mean").asDouble();

            // Extract write metrics
            JsonNode writeNode = jobsNode.path("write");
            int writeBandwidth = writeNode.path("bw").asInt();
            int writeIOPS = (int) writeNode.path("iops").asDouble();
            JsonNode writeLatencyNode = jobsNode.path("clat_ns");
            double writeLatency = writeLatencyNode.path("mean").asDouble();

            // Runtime in nanoseconds
            long durationNs = System.nanoTime() - start;

            // TODO storageBenchmarkTool ??????????
            // Store the metrics in the scores map
            scores.put("read_bandwidth_mb", readBandwidth);
            scores.put("read_iops", readIOPS);
            scores.put("read_latency_ns", (int) readLatency);
            scores.put("write_bandwidth_mb", writeBandwidth);
            scores.put("write_iops", writeIOPS);
            scores.put("write_latency_ns", (int) writeLatency);
            scores.put("total_runtime_ms", (int) (durationNs / 1_000_000));

            log.info("Storage benchmark completed successfully");
            log.info("Storage benchmark completed successfully");
        } catch (IOException | InterruptedException e) {
            log.error("Error running storage benchmark: {}", e.getMessage());
            // Return default values in case of error
            throw new RuntimeException("Storage benchmark failed");
        }
        return scores;
    }

    /**
     * Executes a network benchmark by downloading a fixed-size test file and
     * measuring the effective inbound bandwidth.
     *
     * Since no upload endpoint is available, outbound bandwidth cannot be
     * measured and is reported as -1.
     *
     * @param quick if true, downloads a 10 MB file; otherwise downloads a 100 MB file.
     * @return a map containing:
     */
    public Map<String, Object> benchmarkNetwork(boolean quick) {
        Map<String, Object> scores = new HashMap<>();

        String url = quick
                ? "https://proof.ovh.net/files/10Mb.dat"
                : "https://proof.ovh.net/files/100Mb.dat";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

            long totalBytes = 0;
            long start = System.nanoTime();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream input = response.body()) {
                byte[] buffer = new byte[64 * 1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                }
            }

            long elapsedNs = System.nanoTime() - start;
            double elapsedSeconds = elapsedNs / 1_000_000_000.0;
            int downloadKBps = (int) ((totalBytes / 1024.0) / elapsedSeconds);
            scores.put("download_bandwidth_kbps", downloadKBps);
            scores.put("upload_bandwidth_kbps", -1);
            scores.put("total_runtime_ms", (int) (elapsedNs / 1_000_000));
            log.info("Network benchmark completed. Download={} KB/s, Runtime={} ms", downloadKBps, elapsedNs / 1_000_000);

        } catch (Exception e) {
            log.error("Error running network benchmark", e);
            throw new RuntimeException("Network benchmark failed");
        }
        return scores;
    }
}