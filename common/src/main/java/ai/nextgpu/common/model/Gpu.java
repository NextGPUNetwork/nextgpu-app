package ai.nextgpu.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ai.nextgpu.common.exception.ComponentException;
import ai.nextgpu.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.util.Collection;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "gpu",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gpu_manufacturer_model",
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
public class Gpu extends BaseComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gpu_seq_gen")
    @SequenceGenerator(name = "gpu_seq_gen", sequenceName = "gpu_seq", allocationSize = 1)
    @ColumnDefault("nextval('gpu_seq')")
    private Long id;

    @ManyToMany(mappedBy = "gpus", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @BatchSize(size = 20)
    @Comment("The computers that use this")
    private Collection<Computer> computers;

    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private GpuArchitecture architecture; // Manufacturer's architecture

    private Integer shaderCores; // Shader cores/CUDA cores/Streaming cores

    private Integer tensorCores; // Special cores used in tensor processes

    private Integer minClock; // Base clock speed in MHz

    private Integer maxClock; // Boost clock speed in MHz

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StorageUnit capacityUnit;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MemoryType type; // Only GDDR types allowed

    public void setType(MemoryType type) {
        if (type == null) {
            this.type = MemoryType.UNKNOWN;
            return;
        }
        switch (type) {
            case GDDR4 -> this.type = MemoryType.GDDR4;
            case GDDR5 -> this.type = MemoryType.GDDR5;
            case GDDR5X -> this.type = MemoryType.GDDR5X;
            case GDDR6 -> this.type = MemoryType.GDDR6;
            case GDDR6X -> this.type = MemoryType.GDDR6X;
            default -> this.type = MemoryType.UNKNOWN;
        }
    }

    public void compareForAudit(BaseComponent other)
            throws NullPointerException, IllegalArgumentException, ComponentException {
        if (other == null) throw new NullPointerException();

        // Ensure we're comparing the same type
        if (!(other instanceof Gpu otherGpu)) {
            throw new IllegalArgumentException("Cannot compare Gpu with " + other.getClass().getSimpleName());
        }
        
        if (exceedsTolerance(this.maxClock, otherGpu.maxClock, 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("Gpu minClock exceeds maximum allowed tolerance: expected="+this.minClock+" actual="+otherGpu.minClock)
            );

        if (exceedsTolerance(this.minClock, otherGpu.minClock, 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("Gpu minClock exceeds maximum allowed tolerance: expected="+this.minClock+" actual="+otherGpu.minClock)
            );

//        if (exceedsTolerance(this.getTdpWatts(), otherGpu.getTdpWatts(), 5.0))
//            throw new ComponentException(
//                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
//                    ErrorCode.MAX_TOLERANCE_VIOLATION,
//                    new IllegalArgumentException("Gpu tdpWatts exceeds maximum allowed tolerance: expected="+this.getTdpWatts()+" actual="+otherGpu.getTdpWatts())
//            );
    }

}
