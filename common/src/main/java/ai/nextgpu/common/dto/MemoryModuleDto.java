package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.MemoryModule;
import ai.nextgpu.common.model.MemoryType;
import ai.nextgpu.common.model.StorageUnit;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryModuleDto extends BaseComponentDto {

    private MemoryType type;

    private Integer capacity;

    private StorageUnit capacityUnit;

    private Integer busSpeed;

    public static MemoryModule toEntity (MemoryModuleDto memoryModuleDto){
        if(memoryModuleDto == null) return null;

        MemoryModule memory = new MemoryModule();
        BeanUtils.copyProperties(memoryModuleDto, memory);

        return memory;
    }

    public static MemoryModuleDto toDto(MemoryModule memoryModule) {
        return MemoryModuleDto.builder()
                .uuid(memoryModule.getUuid())
                .model(memoryModule.getModel())
                .manufacturer(memoryModule.getManufacturer())
                .yearReleased(memoryModule.getYearReleased())
                .isDiscontinued(memoryModule.getIsDiscontinued())
                .tdpWatts(memoryModule.getTdpWatts())
                .productIdentifier(memoryModule.getProductIdentifier())
                .dateCreated(memoryModule.getDateCreated())
                .voided(memoryModule.getVoided())
                .dateVoided(memoryModule.getDateVoided())
                .dateUpdated(memoryModule.getDateUpdated())
                .voidReason(memoryModule.getVoidReason())
                .type(memoryModule.getType())
                .capacity(memoryModule.getCapacity())
                .capacityUnit(memoryModule.getCapacityUnit())
                .busSpeed(memoryModule.getBusSpeed())
                .build();
    }
}
