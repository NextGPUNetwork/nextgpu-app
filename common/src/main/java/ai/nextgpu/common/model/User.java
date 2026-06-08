package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@MappedSuperclass
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    @Comment("Username chosen by the User. Should be unique")
    private String username;

    @Enumerated(EnumType.STRING)
    private Role role;
}
