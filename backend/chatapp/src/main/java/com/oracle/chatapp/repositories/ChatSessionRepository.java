package com.oracle.chatapp.repositories;
import com.oracle.chatapp.entities.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ChatSessionRepository extends JpaRepository<ChatSession,Long> { List<ChatSession> findByUserIdOrderByCreatedAtDesc(Integer userId); List<ChatSession> findByEmployeeIdOrderByCreatedAtDesc(Integer employeeId); List<ChatSession> findByStatusOrderByCreatedAtAsc(ChatStatus status); Optional<ChatSession> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(Integer userId, Collection<ChatStatus> statuses); }
