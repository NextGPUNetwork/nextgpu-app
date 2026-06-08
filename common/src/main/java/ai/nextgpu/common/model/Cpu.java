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
@Table(name = "cpu", uniqueConstraints = {@UniqueConstraint(name = "uk_cpu_manufacturer_model", columnNames = {"manufacturer", "model"})})
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Cpu extends BaseComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cpu_seq_gen")
    @SequenceGenerator(name = "cpu_seq_gen", sequenceName = "cpu_seq", allocationSize = 1)
    @ColumnDefault("nextval('cpu_seq')")
    private Long id;

    @ManyToMany(mappedBy = "cpus", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @BatchSize(size = 20)
    @Comment("The computers that use this")
    private Collection<Computer> computers;

    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    @Comment("The underlying architecture like x86_64, ARM, etc.")
    private CpuArchitecture architecture; // Model number

    @Comment("Number of physical cores inside the chip package")
    private Integer cores; // No. of processing cores

    @Comment("Number of logical cores. It is usually double the no. of cores if hyper-threading is enabled")
    private Integer threads; // Number of threads (generally twice as cores)

    @Comment("Minimum CPU frequency. A.k.a. Base Clock in MHz")
    private Integer minClock; // Base clock speed in MHz

    @Comment("Maximum CPU frequency. A.k.a. Boost/Turbo Clock in MHz")
    private Integer maxClock; // Boost clock speed in MHz

    @Column(name = "l3_cache")
    @Comment("The highest level cache in KBs. Usually, it is level 3 or in some cases, level 4 cache")
    private Integer l3Cache; // Size of level-3 cache in KBs

    //    @Override
    public void compareForAudit(BaseComponent other) throws ComponentException, NullPointerException, IllegalArgumentException {
        if (other == null) throw new NullPointerException();

        // Ensure we're comparing the same type
        if (!(other instanceof Cpu otherCpu)) {
           throw new IllegalArgumentException("Cannot compare Cpu with " + other.getClass().getSimpleName());
        }

        // Compare CPU-specific properties with 5% tolerance
        if (!Objects.equals(this.cores, otherCpu.cores))
            throw new ComponentException(
                    ErrorCode.SPECIFICATION_MISMATCH_ERROR.getDescription(),
                    ErrorCode.SPECIFICATION_MISMATCH_ERROR,
                    new IllegalArgumentException("Cpu cores count mismatch: expected="+this.cores+"actual="+otherCpu.cores)
            ); // Must be equal

        if (!Objects.equals(this.threads, otherCpu.threads))
            throw new ComponentException(
                    ErrorCode.SPECIFICATION_MISMATCH_ERROR.getDescription(),
                    ErrorCode.SPECIFICATION_MISMATCH_ERROR,
                    new IllegalArgumentException("Cpu thread count mismatch: expected="+this.threads+"actual="+otherCpu.threads)
            ); // Must be equal

        //noinspection DuplicatedCode
        if (exceedsTolerance(this.maxClock, otherCpu.maxClock, 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("Cpu maxClock exceeds maximum allowed tolerance: expected="+this.maxClock+"actual="+otherCpu.maxClock )
            );

        if (exceedsTolerance(this.minClock, otherCpu.minClock, 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("Cpu minClock exceeds maximum allowed tolerance: expected="+this.minClock+"actual="+otherCpu.minClock)
            );

        if (exceedsTolerance(this.getTdpWatts(), otherCpu.getTdpWatts(), 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("Cpu tdpWatts exceeds maximum allowed tolerance: expected="+this.getTdpWatts()+"actual="+otherCpu.getTdpWatts())
            );

        if (exceedsTolerance(this.l3Cache, otherCpu.l3Cache, 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("Cpu l3Cache exceeds maximum allowed tolerance: expected="+this.l3Cache+"actual="+otherCpu.l3Cache)
            );
    }

}
