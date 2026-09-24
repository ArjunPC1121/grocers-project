package com.oracle.chatapp.repositories;
import com.oracle.chatapp.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
 List<ChatMessage> findBySessionIdOrderBySentAtAsc(Long sessionId);
 Optional<ChatMessage> findFirstBySessionIdAndSenderRoleOrderBySentAtDesc(Long sessionId,String senderRole);
}
