package com.workcopilot.chat.repository;

import com.workcopilot.chat.entity.ChatFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {

    List<ChatFile> findByChatMessageId(Long chatMessageId);
}
