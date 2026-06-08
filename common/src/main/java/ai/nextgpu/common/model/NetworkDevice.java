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
@Table(name = "network_device",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_network_device_manufacturer_model",
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
public class NetworkDevice extends BaseComponent{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "network_device_seq_gen")
    @SequenceGenerator(name = "network_device_seq_gen", sequenceName = "network_device_seq", allocationSize = 1)
    @ColumnDefault("nextval('network_device_seq')")
    private Long id;

    @ManyToMany(mappedBy = "networkDevices", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @BatchSize(size = 20)
    @Comment("The computers that use this")
    private Collection<Computer> computers;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NetworkInterfaceType interfaceType;

    @Comment("Hardware (MAC) address of the network device")
    private String macAddress;

    @Comment("Data transfer speed in Mega bits per second")
    private int speed;  // Mbps

    public void compareForAudit(BaseComponent other) throws NullPointerException, IllegalArgumentException, ComponentException {
        if (other == null) throw new NullPointerException("Component cannot be null.");

        // Ensure we're comparing the same type
        if (!(other instanceof NetworkDevice otherNetworkDevice)) {
            throw new IllegalArgumentException("Cannot compare MemoryModule with " + other.getClass().getSimpleName());
        }

        // Compare bus speed with 5% tolerance
        if (exceedsTolerance(this.speed, otherNetworkDevice.speed, 5.0))
            throw new ComponentException(
                    ErrorCode.MAX_TOLERANCE_VIOLATION.getDescription(),
                    ErrorCode.MAX_TOLERANCE_VIOLATION,
                    new IllegalArgumentException("NetworkDevice speed exceeds maximum allowed tolerance: expected="+this.speed+"actual="+otherNetworkDevice.speed)
            );
    }
}
