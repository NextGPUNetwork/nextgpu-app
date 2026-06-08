package ai.nextgpu.common.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "connection_session")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class ConnectionSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "connection_session_seq_gen")
    @SequenceGenerator(name = "connection_session_seq_gen", sequenceName = "connection_session_seq", allocationSize = 1)
    @ColumnDefault("nextval('connection_session_seq')")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    @ManyToOne
    @JoinColumn(name = "provider_pc_id")
    @Comment("The computer of the provider, providing the service to the given Consumer")
    private Computer providerComputer;

    @Builder.Default
    @Column(nullable = false)
    private Instant startTime = Instant.now();

    @Comment("The time when the session ended, null while the session is active")
    private Instant endTime;

    @Column(name = "disconnect_reason")
    @Comment("The reason for the disconnection, if any")
    private DisconnectReasonCode disconnectReason;
}
