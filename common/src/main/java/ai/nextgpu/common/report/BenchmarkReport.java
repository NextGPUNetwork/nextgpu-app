package ai.nextgpu.common.report;

import com.fasterxml.jackson.databind.JsonNode;
import ai.nextgpu.common.model.Computer;
import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.util.JsonUtil;
import ai.nextgpu.common.util.MapAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Represents a detailed benchmark report for a computing system. Provides benchmark scores and details
 * for various system components such as CPU, GPU, memory, storage, and network.
 * For multiple components (e.g. Memory modules) the results are aggregated, with an exception of GPUs
 *
 * This class extends {@link BaseReport} and includes additional fields to store benchmark-related
 * metrics and methods to export the report in different formats.
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_report")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkReport extends BaseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benchmark_report_seq_gen")
    @SequenceGenerator(name = "benchmark_report_seq_gen", sequenceName = "benchmark_report_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    @Comment("The provider of computer whose benchmark is done")
    private Provider provider;

    @ManyToOne
    @JoinColumn(name = "computer_id")
    @Comment("The computer whose benchmark is done")
    private Computer computer;

    // CPU Benchmark Fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "cpu_benchmark_results", length = 5000)
    private Map<String, Object> cpuBenchmarkResults;

    // GPU Benchmark Fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "gpu_benchmark_results", length = 5000)
    private Map<String, Object> gpuBenchmarkResults;

    // Memory Benchmark Fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "memory_benchmark_results", length = 5000)
    private Map<String, Object> memoryBenchmarkResults;

    // Storage Benchmark Fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "storage_benchmark_results", length = 5000)
    private Map<String, Object> storageBenchmarkResults;

    // Network Benchmark Fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "network_benchmark_results", length = 5000)
    private Map<String, Object> networkBenchmarkResults;

    // Other Benchmark Fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "other_benchmark_results", length = 5000)
    private Map<String, Object> otherBenchmarkResults;

    public void exportToHtml(String filename) {

    }

    public void exportToPdf(String filename) {

    }

    public void exportToText(String filename) {

    }

    @Override
    public JsonNode asJson() {
        return JsonUtil.OBJECT_MAPPER.createObjectNode()
                .putPOJO("cpuBenchmarkResults", cpuBenchmarkResults)
                .putPOJO("gpuBenchmarkResults", gpuBenchmarkResults)
                .putPOJO("memoryBenchmarkResults", memoryBenchmarkResults)
                .putPOJO("storageBenchmarkResults", storageBenchmarkResults)
                .putPOJO("networkBenchmarkResults", networkBenchmarkResults)
                .putPOJO("otherBenchmarkResults", otherBenchmarkResults);
    }
}
