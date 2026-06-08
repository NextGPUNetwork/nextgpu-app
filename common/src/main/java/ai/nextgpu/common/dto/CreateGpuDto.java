package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.GpuArchitecture;
import ai.nextgpu.common.model.MemoryType;
import ai.nextgpu.common.model.StorageUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGpuDto {

    private String manufacturer;
    private String model;
    private String productIdentifier;

    @NotNull(message = "Architecture is required")
    private GpuArchitecture architecture;

    private Integer shaderCores;

    private Integer tensorCores;

    private Integer minClock;

    private Integer maxClock;

    @NotNull(message = "Capacity is required")
    @Min(value = 0, message = "Capacity must be positive")
    private Integer capacity;

    @NotNull(message = "Capacity Unit is required")
    private StorageUnit capacityUnit;

    @NotNull(message = "type is required")
    private MemoryType type;
}
