package ai.nextgpu.common.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.ColumnDefault;

public class ComputerEventType extends BaseMetadata{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "computer_event_type_seq_gen")
    @SequenceGenerator(name = "computer_event_type_seq_gen", sequenceName = "computer_event_type_seq", allocationSize = 1)
    @ColumnDefault("nextval('computer_event_type_seq')")
    private Long id;
}
