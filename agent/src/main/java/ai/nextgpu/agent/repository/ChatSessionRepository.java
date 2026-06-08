package ai.nextgpu.agent.repository;

import ai.nextgpu.agent.model.ChatMessage;
import ai.nextgpu.agent.model.ChatSession;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ChatSession entity operations.
 */
@Repository
public interface ChatSessionRepository extends BaseRepository<ChatSession, Long> {
}
