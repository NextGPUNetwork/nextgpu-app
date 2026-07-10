package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(name = "consumer_attribute_type")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConsumerAttributeType extends BaseMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consumer_attribute_type_seq_gen")
    @SequenceGenerator(name = "consumer_attribute_type_seq_gen", sequenceName = "consumer_attribute_type_seq", allocationSize = 1)
    @ColumnDefault("nextval('consumer_attribute_type_seq')")
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
