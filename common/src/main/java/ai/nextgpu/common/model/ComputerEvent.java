package ai.nextgpu.common.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.ColumnDefault;

public class ComputerEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "computer_event_seq_gen")
    @SequenceGenerator(name = "computer_event_seq_gen", sequenceName = "computer_event_seq", allocationSize = 1)
    @ColumnDefault("nextval('computer_event_seq')")
    private Long id;
}
