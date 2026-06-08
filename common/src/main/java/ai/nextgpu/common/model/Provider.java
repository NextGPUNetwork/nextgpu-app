package ai.nextgpu.common.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(name = "provider")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Provider extends User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "provider_seq_gen")
    @SequenceGenerator(name = "provider_seq_gen", sequenceName = "provider_seq", allocationSize = 1)
    @ColumnDefault("nextval('provider_seq')")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Wallet address used to Sign-up")
    private String walletAddress;

    @Column(nullable = false)
    @Comment("Email address is applicable only to Providers")
    private String providerEmail;

    @Comment("City part of provider address")
    private String city;

    @Comment("Country part of provider address")
    private String country;
}
