package ai.nextgpu.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ai.nextgpu.common.exception.ComponentException;
import ai.nextgpu.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.beans.InvalidPropertyException;

import java.util.Collection;

@Getter
@Setter
@Entity
@Table(name = "memory_module",
        uniqueConstraints = {
                @UniqueConstraint(
                    name = "uk_memory_module_manufacturer_model",
                        columnNames = {
                            "manufacturer",
                            "model"
                        }
                )
        }
)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class MemoryModule extends BaseComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memory_module_seq_gen")
    @SequenceGenerator(name = "memory_module_seq_gen", sequenceName = "memory_module_seq", allocationSize = 1)
    @ColumnDefault("nextval('memory_module_seq')")
    private Long id;

    @ManyToMany(mappedBy = "memories", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @BatchSize(size = 20)
    @Comment("The computers that use this")
    private Collection<Computer> computers;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Comment("Memory type, e.g. DDR3, DDR4, etc.")
    private MemoryType type;

    @Comment("Capacity of memory module")
    @Column
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StorageUnit capacityUnit;

    @Comment("Front side bus speed (clock) in MHz")
    private Integer busSpeed; // Clock speed in MHz

    public void compareForAudit(BaseComponent other) throws NullPointerException, IllegalArgumentException, ComponentException {
        if (other == null) throw  new NullPointerException("Component cannot be null.");

        // Ensure we're comparing the same type
        if (!(other instanceof MemoryModule otherMemoryModule)) {
            throw new IllegalArgumentException("Cannot compare MemoryModule with " + other.getClass().getSimpleName());
        }

        // Compare bus speed with 5% tolerance
        if (exceedsTolerance(this.busSpeed, otherMemoryModule.busSpeed, 15.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("MemoryModule busSpeed exceeds maximum allowed tolerance: expected="+this.busSpeed+" actual="+otherMemoryModule.busSpeed)
            );
    }
}
