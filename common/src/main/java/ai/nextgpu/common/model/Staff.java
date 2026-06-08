package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Entity
@Table(name = "staff")
@NoArgsConstructor
@ToString
public class Staff extends User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "staff_seq_gen")
    @SequenceGenerator(name = "staff_seq_gen", sequenceName = "staff_seq", allocationSize = 1)
    @ColumnDefault("nextval('staff_seq')")
    private Long id;
}
