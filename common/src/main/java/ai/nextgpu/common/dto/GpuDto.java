package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Gpu;
import ai.nextgpu.common.model.GpuArchitecture;
import ai.nextgpu.common.model.MemoryType;
import ai.nextgpu.common.model.StorageUnit;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GpuDto extends BaseComponentDto{

    private GpuArchitecture architecture;

    private Integer shaderCores;

    private Integer tensorCores;

    private Integer minClock;

    private Integer maxClock;

    private Integer capacity;

    private StorageUnit capacityUnit;

    private MemoryType type;

    public static Gpu toEntity(GpuDto gpuDto) {
        if (gpuDto == null) return null;

        Gpu gpu = new Gpu();
        BeanUtils.copyProperties(gpuDto, gpu);

        return gpu;
    }

    public static GpuDto toDto(Gpu gpu) {
        return GpuDto.builder()
                .uuid(gpu.getUuid())
                .model(gpu.getModel())
                .manufacturer(gpu.getManufacturer())
                .yearReleased(gpu.getYearReleased())
                .isDiscontinued(gpu.getIsDiscontinued())
                .tdpWatts(gpu.getTdpWatts())
                .productIdentifier(gpu.getProductIdentifier())
                .dateCreated(gpu.getDateCreated())
                .voided(gpu.getVoided())
                .dateVoided(gpu.getDateVoided())
                .dateUpdated(gpu.getDateUpdated())
                .voidReason(gpu.getVoidReason())
                .architecture(gpu.getArchitecture())
                .shaderCores(gpu.getShaderCores())
                .tensorCores(gpu.getTensorCores())
                .minClock(gpu.getMinClock())
                .maxClock(gpu.getMaxClock())
                .capacity(gpu.getCapacity())
                .capacityUnit(gpu.getCapacityUnit())
                .type(gpu.getType())
                .build();
    }
}
