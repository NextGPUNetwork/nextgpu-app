package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "global_property")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class GlobalProperty extends BaseMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_property_seq_gen")
    @SequenceGenerator(name = "global_property_seq_gen", sequenceName = "global_property_seq", allocationSize = 1)
    @ColumnDefault("nextval('global_property_seq')")
    private Long id;

    @Setter
    @Column(nullable = false, length = 8192)
    private String valueReference;

    public <T> T getValue() {
        return super.getCleanValue(valueReference);
    }
}
