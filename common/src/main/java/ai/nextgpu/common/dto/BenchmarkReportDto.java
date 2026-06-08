package ai.nextgpu.common.dto;

import ai.nextgpu.common.report.BenchmarkReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BenchmarkReportDto extends BaseReportDto {

    private String walletAddress;

    private String computerUuid;

    private Map<String, Object> cpuBenchmarkResults;

    private Map<String, Object> gpuBenchmarkResults;

    private Map<String, Object> memoryBenchmarkResults;

    private Map<String, Object> storageBenchmarkResults;

    private Map<String, Object> networkBenchmarkResults;

    private Map<String, Object> otherBenchmarkResults;

    public static BenchmarkReportDto toDto(BenchmarkReport benchmarkReport){
        return BenchmarkReportDto.builder()
                .uuid(benchmarkReport.getUuid())
                .walletAddress(benchmarkReport.getProvider().getWalletAddress())
                .computerUuid(benchmarkReport.getComputer().getUuid())
                .cpuBenchmarkResults(benchmarkReport.getCpuBenchmarkResults())
                .gpuBenchmarkResults(benchmarkReport.getGpuBenchmarkResults())
                .memoryBenchmarkResults(benchmarkReport.getMemoryBenchmarkResults())
                .storageBenchmarkResults(benchmarkReport.getStorageBenchmarkResults())
                .networkBenchmarkResults(benchmarkReport.getNetworkBenchmarkResults())
                .otherBenchmarkResults(benchmarkReport.getOtherBenchmarkResults())
                .dateCreated(benchmarkReport.getDateCreated())
                .description(benchmarkReport.getDescription())
                .elapsedTime(benchmarkReport.getElapsedTime())
                .build();
    }
}
