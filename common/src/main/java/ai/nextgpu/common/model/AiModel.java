package ai.nextgpu.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.common.util.ListAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ai_model")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class AiModel extends BaseObject implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_model_seq_gen")
    @SequenceGenerator(name = "ai_model_seq_gen", sequenceName = "ai_model_seq", allocationSize = 1)
    @ColumnDefault("nextval('ai_model_seq')")
    @Comment("Primary key")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Comment("The registry which lists this model (e.g. Ollama, Huggingface, etc.)")
    private AiModelRegistry registry;

    @Column(length = 50, unique = true, nullable = false)
    @Comment("Qualified model name, which may be used as a reference in the model registry (e.g. deepseek-r1:1.5b, qwen3-vl:32b, et.c)")
    private String model;

    @Comment("Full name for display on interfaces (e.g. Meta Llama 3.1 - 8B)")
    private String fullName;

    @Comment("Description")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Convert(converter = ListAttributeConverter.class)
    @Column(columnDefinition = "TEXT")
    @Comment("The tasks that the model is known to perform (e.g. programming, translation, etc.)")
    private List<String> tasks;

    @Comment("Minimum memory of the GPU/CPU required by the model to load")
    private Integer requiredVram;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Comment("Unit of GPU/CPU memory required")
    private StorageUnit vramUnit;

    @Comment("The sampler for ComfyUI workflow")
    private String sampler;

    @Comment("The scheduler for ComfyUI workflow")
    private String scheduler;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(length = 5000)
    @Comment("The files required by the model for ComfyUI workflow")
    private List<ComfyUiModelFile> files;

    @Column
    @Comment("The size of the model file")
    private Integer size;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Comment("Unit of the model size")
    private StorageUnit sizeUnit;

    @Column
    @Comment("The type of the model (e.g. general, vision, etc.)")
    private String type;

    @Column
    @Comment("The maximum number of tokens in the context window")
    private Integer contextTokens;

    public static AiModel fromJson(String json) {
        try {
            return objectMapper.readValue(json, AiModel.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Computer JSON", e);
        }
    }
}
