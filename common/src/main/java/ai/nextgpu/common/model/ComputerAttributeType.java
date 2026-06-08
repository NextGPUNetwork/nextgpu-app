package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(name = "computer_attribute_type")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ComputerAttributeType extends BaseMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "computer_attribute_type_seq_gen")
    @SequenceGenerator(name = "computer_attribute_type_seq_gen", sequenceName = "computer_attribute_type_seq", allocationSize = 1)
    @ColumnDefault("nextval('computer_attribute_type_seq')")
    private Long id;

    @Column
    @Comment("Whether this attribute type will be included in search filters or not")
    private Boolean isSearchable;

    @Column
    @Comment("Is this attribute type mandatory to save a Computer entity?")
    private Boolean isMandatory;

    @Column
    @Comment("Can this attribute type be duplicated? True, by default")
    private Boolean isUnique;

    @Column
    @Comment("Regular expression to validate the value entered in the respective ComputerAttribute object")
    private String validationRegex;

    @Column
    @Comment("Useful in UI to define the order in which this attribute will be displayed in forms and reports")
    private Integer displayOrder;

    @Column
    @Comment("Useful in UI. Combine with displayOrder to create sections on UI interface")
    private String category;
}
