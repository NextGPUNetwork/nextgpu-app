package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Cpu;
import ai.nextgpu.common.model.CpuArchitecture;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CpuDto extends BaseComponentDto {
    private CpuArchitecture architecture;

    private Integer cores;

    private Integer threads;

    private Integer minClock;

    private Integer maxClock;

    private Integer l3Cache;

    public static Cpu toEntity(CpuDto cpuDto){
        if (cpuDto == null) return null;

        Cpu cpu = new Cpu();
        BeanUtils.copyProperties(cpuDto, cpu);

        return cpu;
    }

    public static CpuDto toDto(Cpu cpu) {
        return CpuDto.builder()
                .uuid(cpu.getUuid())
                .model(cpu.getModel())
                .manufacturer(cpu.getManufacturer())
                .yearReleased(cpu.getYearReleased())
                .isDiscontinued(cpu.getIsDiscontinued())
                .tdpWatts(cpu.getTdpWatts())
                .productIdentifier(cpu.getProductIdentifier())
                .dateCreated(cpu.getDateCreated())
                .voided(cpu.getVoided())
                .dateVoided(cpu.getDateVoided())
                .dateUpdated(cpu.getDateUpdated())
                .voidReason(cpu.getVoidReason())
                .architecture(cpu.getArchitecture())
                .cores(cpu.getCores())
                .threads(cpu.getThreads())
                .minClock(cpu.getMinClock())
                .maxClock(cpu.getMaxClock())
                .l3Cache(cpu.getL3Cache())
                .build();
    }
}
