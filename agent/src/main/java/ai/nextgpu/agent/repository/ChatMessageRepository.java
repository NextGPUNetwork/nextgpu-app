package ai.nextgpu.agent.repository;

import ai.nextgpu.agent.model.ChatMessage;
import ai.nextgpu.agent.model.ChatSession; // Import ChatSession
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ChatMessage entity operations.
 */
@Repository
public interface ChatMessageRepository extends BaseRepository<ChatMessage, Long> {

    //TODO: Refactor this using @Query and also input pinned as parameter
    long countByChatSessionAndPinnedTrue(ChatSession chatSession);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession = :chatSession AND cm.content LIKE %:pattern%")
    List<ChatMessage> findByContent(@Param("chatSession") ChatSession chatSession, @Param("pattern") String pattern);
}
