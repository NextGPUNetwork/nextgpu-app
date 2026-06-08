package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.AiModel;
import ai.nextgpu.common.model.ComfyUiModelFile;
import ai.nextgpu.common.model.AiModelRegistry;
import ai.nextgpu.common.model.StorageUnit;
import ai.nextgpu.common.util.JsonUtil;
import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelDto {
    private String model;
    private String fullName;
    private String description;
    private String[] tasks;
    private Integer requiredVramInGB;
    private String modelRegistry;
    private Integer sizeInGB;
    private String type;
    private Integer contextTokens;
    private String sampler;
    private String scheduler;
    private List<ComfyUiModelFile> files;


    public static AiModel toEntity(AiModelDto dto) {
        if (dto == null) return null;
        List<String> taskList = dto.getTasks() == null ? null : Arrays.asList(dto.getTasks());
        return AiModel.builder()
                .registry(dto.getModelRegistry() == null ? AiModelRegistry.OLLAMA : AiModelRegistry.valueOf(dto.getModelRegistry()))
                .model(dto.getModel())
                .fullName(dto.getFullName())
                .description(dto.getDescription())
                .tasks(taskList)
                .requiredVram(dto.getRequiredVramInGB())
                .vramUnit(StorageUnit.GIGABYTE)
                .size(dto.getSizeInGB())
                .sizeUnit(StorageUnit.GIGABYTE)
                .type(dto.getType())
                .contextTokens(dto.getContextTokens())
                .sampler(dto.getSampler())
                .scheduler(dto.getScheduler())
                .files(JsonUtil.OBJECT_MAPPER.convertValue(
                        dto.getFiles(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<ComfyUiModelFile>>() {
                        }
                ))
                .build();
    }

    public static AiModelDto toDto(AiModel model) {
        if (model == null) return null;
        AiModelDto dto = new AiModelDto();
        dto.setModel(model.getModel());
        dto.setFullName(model.getFullName());
        dto.setDescription(model.getDescription());
        dto.setTasks(model.getTasks() == null ? null : model.getTasks().toArray(new String[0]));
        dto.setRequiredVramInGB(model.getRequiredVram());
        // Only map size when the size unit is GB or unit is not specified
        if (model.getSize() != null && (model.getSizeUnit() == null || Objects.equals(model.getSizeUnit(), StorageUnit.GIGABYTE))) {
            dto.setSizeInGB(model.getSize());
        } else {
            dto.setSizeInGB(null);
        }
        dto.setModelRegistry(model.getRegistry().name());
        dto.setType(model.getType());
        dto.setContextTokens(model.getContextTokens());
        dto.setSampler(model.getSampler());
        dto.setScheduler(model.getScheduler());
        dto.setFiles(JsonUtil.OBJECT_MAPPER.convertValue(
                model.getFiles(),
                new com.fasterxml.jackson.core.type.TypeReference<List<ComfyUiModelFile>>() {}
        ));
        return dto;
    }
}
