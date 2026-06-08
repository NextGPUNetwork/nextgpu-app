package ai.nextgpu.agent.model;

import ai.nextgpu.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column
    public String name;

    @Column(length = 4096)
    public String instructions;

    @OneToMany(mappedBy = "project", cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    public List<ChatSession> chatSessions = new ArrayList<>();
}
