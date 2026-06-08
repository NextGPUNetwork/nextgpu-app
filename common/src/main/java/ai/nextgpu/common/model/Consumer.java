package ai.nextgpu.common.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Data
@Entity
@Table(name = "consumer")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class Consumer extends User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consumer_seq_gen")
    @SequenceGenerator(name = "consumer_seq_gen", sequenceName = "consumer_seq", allocationSize = 1)
    @ColumnDefault("nextval('consumer_seq')")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Wallet address used to Sign-up")
    private String walletAddress;
}
