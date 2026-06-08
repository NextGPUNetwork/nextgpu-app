package ai.nextgpu.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ai.nextgpu.common.exception.ComponentException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.util.Collection;

@Getter
@Setter
@Entity
@Table(name = "generic_component")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class GenericComponent extends BaseComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "generic_component_seq")
    @SequenceGenerator(name = "generic_component_seq", sequenceName = "generic_component_seq", allocationSize = 1)
    @ColumnDefault("nextval('generic_component_seq')")
    private Long id;

    @ManyToMany(mappedBy = "otherComponents", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @BatchSize(size = 20)
    @Comment("The computers that use this")
    private Collection<Computer> computers;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DeviceType type;

    @Column(nullable = false, length = 50)
    @Comment("Name most important specification for this component.")
    private String specificationKey;

    @Comment("Specification value")
    private String specificationValue;

    public void compareForAudit(BaseComponent other) throws NullPointerException, IllegalArgumentException, ComponentException {
        // TODO: Proper implementation of the Generic component
    }
}
