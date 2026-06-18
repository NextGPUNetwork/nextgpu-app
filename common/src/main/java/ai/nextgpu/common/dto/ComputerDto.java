package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Computer;
import ai.nextgpu.common.model.ComputerAttributeType;
import ai.nextgpu.common.model.ComputerType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComputerDto extends BaseDto {

    @NotNull(message = "Computer type is required")
    private ComputerType type;

    @NotEmpty(message = "At least one CPU is required")
    private Collection<CpuDto> cpus;

    @NotEmpty(message = "At least one memory module is required")
    private Collection<MemoryModuleDto> memoryModules;

    @NotEmpty(message = "At least one storage is required")
    private Collection<StorageDto> storages;

    @NotEmpty(message = "At least one GPU is required")
    private Collection<GpuDto> gpus;

    @NotEmpty(message = "Operating system is required")
    private String operatingSystem;

    @NotEmpty(message = "At least one network device is required")
    private Collection<NetworkDeviceDto> networkDevices;

    private Collection<GenericComponentDto> otherComponents;

    private Map<String, String> computerAttributes;

    public static ComputerDto toDto(Computer computer){
        if (computer == null) return null;

        return ComputerDto.builder()
                .uuid(computer.getUuid())
                .dateCreated(computer.getDateCreated())
                .voided(computer.getVoided())
                .dateVoided(computer.getDateVoided())
                .dateUpdated(computer.getDateUpdated())
                .voidReason(computer.getVoidReason())
                .type(computer.getType())
                .operatingSystem(computer.getOperatingSystem())
                .cpus(toDtos(computer.getCpus(), CpuDto::toDto))
                .gpus(toDtos(computer.getGpus(), GpuDto::toDto))
                .storages(toDtos(computer.getStorages(), StorageDto::toDto))
                .memoryModules(toDtos(computer.getMemories(), MemoryModuleDto::toDto))
                .networkDevices(toDtos(computer.getNetworkDevices(), NetworkDeviceDto::toDto))
                .otherComponents(toDtos(computer.getOtherComponents(), GenericComponentDto::toDto))
                .computerAttributes(toComputerAttributeDtosMap(computer.getComputerAttributes()))
                .build();
    }

    /**
     * This method sets all the computer properties except the computer attributes.
     * There is another method in ComputerService to set the attributes from DTO
     *
     * @param computerDto
     * @return Computer
     */
    public static Computer toEntity(ComputerDto computerDto){
        Computer computer = new Computer();
        BeanUtils.copyProperties(computerDto, computer,
                "cpus", "gpus", "storages", "memories", "memoryModules", "networkDevices",
                "otherComponents", "computerAttributes");
        // Handle collections separately (different types/names)
        computer.setCpus(toEntities(computerDto.getCpus(), CpuDto::toEntity));
        computer.setGpus(toEntities(computerDto.getGpus(), GpuDto::toEntity));
        computer.setStorages(toEntities(computerDto.getStorages(), StorageDto::toEntity));
        computer.setMemories(toEntities(computerDto.getMemoryModules(), MemoryModuleDto::toEntity));
        computer.setNetworkDevices(toEntities(computerDto.getNetworkDevices(), NetworkDeviceDto::toEntity));
        computer.setOtherComponents(toEntities(computerDto.getOtherComponents(), GenericComponentDto::toEntity));
        return computer;
    }

    public static  <T extends BaseComponentDto, D> Set<D> toEntities(Collection<T> dtos, Function<T, D> converter){
        return mapEntities(dtos, converter);
    }

    public static  <T, D> List<D> toDtos(Set<T> entities, Function<T, D> converter){
        return mapDtos(entities, converter);
    }

    public static  <E, D> Set<D> mapEntities(Collection<E> source, Function<E, D> mapper) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        return source.stream().map(mapper).collect(Collectors.toSet());
    }

    public static  <E, D> List<D> mapDtos(Collection<E> source, Function<E, D> mapper) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream().map(mapper).toList();
    }

    public static Map<String, String> toComputerAttributeDtosMap(
            Map<ComputerAttributeType, String> attributeMap) {

        // Create new map
        Map<String , String> result = new HashMap<>();

        // Iterate and convert
        for (Map.Entry<ComputerAttributeType, String> entry : attributeMap.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue());
        }

        return result;
    }
}
