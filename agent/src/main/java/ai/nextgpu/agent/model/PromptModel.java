package ai.nextgpu.agent.model;

import ai.nextgpu.common.model.AiModelRegistry;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PromptModel {

    public String name;

    public String modifiedAt;

    public String digest;

    public String useCase;

    public Float sizeInGB;

    public String shortDescription;

    public String aiModelRegistry;

    public PromptModel(String name, String modifiedAt, String digest, String aiModelRegistry) {
        this.name = name;
        this.modifiedAt = modifiedAt;
        this.digest = digest;
        this.aiModelRegistry = aiModelRegistry;
    }
}
