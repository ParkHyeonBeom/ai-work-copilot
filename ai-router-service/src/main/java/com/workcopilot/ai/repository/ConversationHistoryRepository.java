package com.workcopilot.ai.repository;

import com.workcopilot.ai.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {

    List<ConversationHistory> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(Long userId);

    Optional<ConversationHistory> findByIdAndUserId(Long id, Long userId);
}
