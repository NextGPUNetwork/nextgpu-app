package ai.nextgpu.agent.model;

import ai.nextgpu.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_session")
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public boolean starred = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    public Project project;

    @Column
    public String promptModel;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.chatSession = this;
        setDateUpdated(LocalDateTime.now());
    }
}
