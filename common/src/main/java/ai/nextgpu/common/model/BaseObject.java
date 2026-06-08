package ai.nextgpu.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.UUID;

/**
 * Every persistent entity must inherit this class.
 */
@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@ToString
public abstract class BaseObject {

    @Column(unique = true, nullable = false, length = 38, updatable = false)
    @Comment("Globally unique identifier")
    private String uuid = UUID.randomUUID().toString();
}
