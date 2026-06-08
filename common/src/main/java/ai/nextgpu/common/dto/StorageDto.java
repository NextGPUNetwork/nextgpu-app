package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Storage;
import ai.nextgpu.common.model.StorageType;
import ai.nextgpu.common.model.StorageUnit;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StorageDto extends BaseComponentDto {

    private StorageType type;

    private Integer capacity;

    private StorageUnit capacityUnit;

    private Integer cache;

    private Integer readSpeed;

    private Integer writeSpeed;

    private Boolean isRemovable;

    public static Storage toEntity (StorageDto storageDto){
        if(storageDto == null) return null;

        Storage storage = new Storage();
        BeanUtils.copyProperties(storageDto, storage);

        return storage;
    }

    public static StorageDto toDto(Storage storage) {
        return StorageDto.builder()
                .uuid(storage.getUuid())
                .model(storage.getModel())
                .manufacturer(storage.getManufacturer())
                .yearReleased(storage.getYearReleased())
                .isDiscontinued(storage.getIsDiscontinued())
                .tdpWatts(storage.getTdpWatts())
                .productIdentifier(storage.getProductIdentifier())
                .dateCreated(storage.getDateCreated())
                .voided(storage.getVoided())
                .dateVoided(storage.getDateVoided())
                .dateUpdated(storage.getDateUpdated())
                .voidReason(storage.getVoidReason())
                .type(storage.getType())
                .capacity(storage.getCapacity())
                .capacityUnit(storage.getCapacityUnit())
                .cache(storage.getCache())
                .readSpeed(storage.getReadSpeed())
                .writeSpeed(storage.getWriteSpeed())
                .isRemovable(storage.getIsRemovable())
                .build();
    }
}

