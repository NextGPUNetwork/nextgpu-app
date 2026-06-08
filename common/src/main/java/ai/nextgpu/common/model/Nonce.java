package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "nonce")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Nonce extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nonce_seq_gen")
    @SequenceGenerator(name = "nonce_seq_gen", sequenceName = "nonce_seq", allocationSize = 1)
    @ColumnDefault("nextval('nonce_seq')")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("The wallet address whom this nonce belongs to")
    private String walletAddress;

    @Column(nullable = false)
    @Comment("Nonce used to verify the wallet")
    private String nonce;
}
