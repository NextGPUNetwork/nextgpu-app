package ai.nextgpu.common.report;

import com.fasterxml.jackson.databind.JsonNode;
import ai.nextgpu.common.model.BaseObject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseReport extends BaseObject implements Exportable {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss");

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    private String description;

    private Long elapsedTime;

    /**
     * Serializes this report's content to a {@link JsonNode} tree, e.g. for returning in an
     * API response or feeding into another export format.
     *
     * @return the report content as JSON, or {@code null} if this report type does not yet
     *         support JSON serialization
     */
    public abstract JsonNode asJson();
}
