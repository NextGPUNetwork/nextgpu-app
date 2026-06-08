// CreateCpuDto.java
package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.CpuArchitecture;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCpuDto {

    @NotNull(message = "architecture count is required")
    private CpuArchitecture architecture;

    @NotNull(message = "cores is required")
    @Min(value = 1, message = "cores must be at least 1")
    private Integer cores;

    private Integer threads;

    private Integer minClock;

    private Integer maxClock;

    private Integer l3Cache;
}
