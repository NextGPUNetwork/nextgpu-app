package ai.nextgpu.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.common.util.AuditEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EntityListeners(AuditEntityListener.class)
public abstract class BaseEntity extends BaseObject implements Serializable {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @Setter
    @Column(length = 255)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Setter
    private Boolean voided = false;

    @Setter
    @LastModifiedDate
    private LocalDateTime dateUpdated;

    @Setter
    private LocalDateTime dateVoided;

    @Setter
    private String voidReason;

    @Setter
    private String description;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseEntity that)) return false;
        return Objects.equals(getUuid(), that.getUuid()) && Objects.equals(name, that.name) && Objects.equals(dateCreated, that.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), name, dateCreated);
    }
}
