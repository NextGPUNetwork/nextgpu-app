package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "computer")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class Computer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "computer_seq_gen")
    @SequenceGenerator(name = "computer_seq_gen", sequenceName = "computer_seq", allocationSize = 1)
    @ColumnDefault("nextval('computer_seq')")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    @Comment("The provider of computer")
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Comment("The type of the computer (e.g., Laptop, Desktop, Workstation)")
    private ComputerType type;

    @BatchSize(size = 10)
    @ManyToMany
    @JoinTable(name = "computer_cpu",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "cpu_id", nullable = false)
    )
    @Comment("The collection of CPUs active. Applicable mainly on Workstations and Servers")
    @ToString.Exclude
    private Set<Cpu> cpus;

    @BatchSize(size = 10)
    @ManyToMany
    @JoinTable(name = "computer_memory",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "memory_id", nullable = false)
    )
    @Comment("The collection of memory modules active")
    @ToString.Exclude
    private Set<MemoryModule> memories;

    @BatchSize(size = 10)
    @ManyToMany
    @JoinTable(name = "computer_storage",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "storage_id", nullable = false)
    )
    @Comment("The collection of storages. Generally, there is SSD boot drive and HDDs for data")
    @ToString.Exclude
    private Set<Storage> storages;

    @BatchSize(size = 10)
    @ManyToMany
    @JoinTable(name = "computer_gpu",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "gpu_id", nullable = false)
    )
    @Comment("The collection of GPUs. There must be at least 1 (even if integrated) GPU")
    @ToString.Exclude
    private Set<Gpu> gpus;

    @Column(name = "operating_system", length = 255)
    @Comment("The operating system installed on this computer, e.g., Windows 11 Pro, Ubuntu 24.04 LTS")
    private String operatingSystem;

    @Column(name = "hardware_fingerprint", length = 64, unique = true)
    @Comment("SHA-256 hash (hex encoded, 64 characters) of the unique SMBIOS UUID, BIOS serial number, Motherboard serial number, TPM identifier (if available), Primary disk serial number. Used for hardware fingerprinting. Fallback sequence: System Serial -> Motherboard Baseboard Serial.")
    private String hardwareFingerprint;

    @BatchSize(size = 10)
    @ManyToMany
    @JoinTable(name = "computer_nics",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "nic_id", nullable = false)
    )
    @Comment("The collection of NICs attached")
    @ToString.Exclude
    private Set<NetworkDevice> networkDevices;

    @BatchSize(size = 10)
    @ManyToMany
    @JoinTable(name = "computer_other_component",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "component_id", nullable = false)
    )
    @Comment("The collection of other peripherals, sensors and components attached to this computer")
    @ToString.Exclude
    private Set<GenericComponent> otherComponents;

    @BatchSize(size = 10)
    @ElementCollection
    @CollectionTable(
            name = "computer_attribute",
            joinColumns = @JoinColumn(name = "computer_id", nullable = false)
    )
    @MapKeyJoinColumn(name = "computer_attribute_id", nullable = false)
    @Column(name = "attribute_value")
    @Comment("This map collects various attributes of this computer. Anything that cannot be represented as computer parts can be categorized as attribute")
    @ToString.Exclude
    private Map<ComputerAttributeType, String> computerAttributes;


    public static Computer fromJson(String json) {
        try {
            return objectMapper.readValue(json, Computer.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Computer JSON", e);
        }
    }

    public void addOtherComponents(Set<GenericComponent> component) {
        if (this.otherComponents == null) {
            this.otherComponents = new HashSet<>();
        }
        this.otherComponents.addAll(component);
    }
}
