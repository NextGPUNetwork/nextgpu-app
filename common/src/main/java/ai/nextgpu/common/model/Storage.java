package ai.nextgpu.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.util.Collection;

@Table(name = "storage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_storage_manufacturer_model",
                        columnNames = {
                                "manufacturer",
                                "model"
                        }
                )
        }
)
@Getter
@Setter
@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Storage extends BaseComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_seq_gen")
    @SequenceGenerator(name = "storage_seq_gen", sequenceName = "storage_seq", allocationSize = 1)
    @ColumnDefault("nextval('storage_seq')")
    private Long id;

    @ManyToMany(mappedBy = "storages", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @BatchSize(size = 20)
    @Comment("The computers that use this")
    private Collection<Computer> computers;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StorageType type; // HDD, SSD

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private StorageUnit capacityUnit;

    @Column
    private Integer cache; // NAND cache, Optane, or HDD's internal cache size in MB

    @Transient
    private Integer readSpeed;  // No need to store in the DB. Fill in on the fly

    @Transient
    private Integer writeSpeed; // No need to store in the DB. Fill in on the fly

    @Column
    private Boolean isRemovable;

    public void compareForAudit(BaseComponent other) throws NullPointerException, IllegalArgumentException  {
        if (other == null) throw new NullPointerException("Component cannot be null.");

        // Ensure we're comparing the same type
        if (!(other instanceof Storage otherStorage)) {
            throw new IllegalArgumentException("Cannot compare Storage with " + other.getClass().getSimpleName());
        }
    }

}
