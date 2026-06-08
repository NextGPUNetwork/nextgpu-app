package ai.nextgpu.agent.model;

import java.util.StringTokenizer;

import ai.nextgpu.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_message")
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;

    @ManyToOne
    @JoinColumn(name = "chat_session_id")
    public ChatSession chatSession;

    public String role; // "system", "user", or "assistant"

    @Column(columnDefinition = "LONGTEXT")
    public String content; // the actual content as raw text

    public int index;  // index of this individual message in chat history

    @Column(columnDefinition = "LONGTEXT")
    public String prunedContent; // when pruned, the content is summarized and stored here

    public Boolean pinned = false;  // whether this message is pinned in the chat history

    public int tokenLength;  // length of content in bytes

    /**
     * @param role
     * @param content
     * @deprecated should be removed once the chat history is being maintained in the DB
     */
    @Deprecated
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.index = 0;
        this.prunedContent = content;
        this.pinned = false;
        this.tokenLength = new StringTokenizer(content, " ").countTokens();
    }

    public ChatMessage(String role, String content, int index, Boolean pinned) {
        this.role = role;
        this.content = content;
        this.index = index;
        this.prunedContent = content;
        this.pinned = pinned;
        this.tokenLength = new StringTokenizer(content, " ").countTokens();
    }

    public Long getId() {
        return chatMessageId;
    }

    public void setId(Long id) {
        this.chatMessageId = id;
    }
}
