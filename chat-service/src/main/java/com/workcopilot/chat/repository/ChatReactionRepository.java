package com.workcopilot.chat.repository;

import com.workcopilot.chat.entity.ChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatReactionRepository extends JpaRepository<ChatReaction, Long> {

    List<ChatReaction> findByChatMessageId(Long chatMessageId);

    Optional<ChatReaction> findByChatMessageIdAndUserIdAndEmoji(Long chatMessageId, Long userId, String emoji);

    List<ChatReaction> findByChatMessageIdIn(List<Long> chatMessageIds);
}
