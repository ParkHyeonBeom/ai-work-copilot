package com.workcopilot.chat.service;

import com.workcopilot.chat.entity.ChatFile;
import com.workcopilot.chat.repository.ChatFileRepository;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_MIME_PREFIXES = Set.of(
            "image/",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
            "text/"
    );

    private final ChatFileRepository chatFileRepository;

    @Value("${chat.file-dir}")
    private String fileDir;

    public ChatFile uploadFile(Long roomId, MultipartFile file) {
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.CHAT_FILE_TOO_LARGE);
        }

        // Validate MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !isAllowedMimeType(mimeType)) {
            throw new BusinessException(ErrorCode.CHAT_FILE_TYPE_NOT_ALLOWED);
        }

        String originalFileName = file.getOriginalFilename();
        String storedFileName = UUID.randomUUID() + "_" + originalFileName;
        Path dirPath = Paths.get(fileDir, String.valueOf(roomId));
        Path filePath = dirPath.resolve(storedFileName);

        try {
            Files.createDirectories(dirPath);
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
        }

        ChatFile chatFile = ChatFile.builder()
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath.toString())
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .build();

        chatFileRepository.save(chatFile);
        log.info("파일 업로드 완료: roomId={}, fileName={}, size={}", roomId, originalFileName, file.getSize());

        return chatFile;
    }

    public ChatFile getFile(Long fileId) {
        return chatFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다."));
    }

    public Resource downloadFile(Long fileId) {
        ChatFile chatFile = chatFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다."));

        Path filePath = Paths.get(chatFile.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "파일이 서버에 존재하지 않습니다.");
        }

        return new FileSystemResource(filePath);
    }

    private boolean isAllowedMimeType(String mimeType) {
        return ALLOWED_MIME_PREFIXES.stream()
                .anyMatch(mimeType::startsWith);
    }
}
