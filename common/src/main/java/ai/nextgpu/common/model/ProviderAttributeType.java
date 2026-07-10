package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "provider_attribute_type")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProviderAttributeType extends BaseMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "provider_attribute_type_seq_gen")
    @SequenceGenerator(name = "provider_attribute_type_seq_gen", sequenceName = "provider_attribute_type_seq", allocationSize = 1)
    @ColumnDefault("nextval('provider_attribute_type_seq')")
    private Long id;

    @Column
    @Comment("Is this attribute type mandatory to save a User entity?")
    private Boolean isMandatory;

    @Column
    @Comment("Can this attribute type be duplicated? True, by default")
    private Boolean isUnique;

    @Column
    @Comment("Regular expression to validate the value entered in the respective UserAttribute object")
    private String validationRegex;

}
