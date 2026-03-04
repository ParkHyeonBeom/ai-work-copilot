package com.workcopilot.chat.controller;

import com.workcopilot.chat.dto.FileUploadResponse;
import com.workcopilot.chat.entity.ChatFile;
import com.workcopilot.chat.service.ChatFileService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/chat/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final ChatFileService chatFileService;

    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> uploadFile(@RequestParam Long roomId,
                                                       @RequestParam("file") MultipartFile file) {
        ChatFile chatFile = chatFileService.uploadFile(roomId, file);

        FileUploadResponse response = new FileUploadResponse(
                chatFile.getId(),
                chatFile.getOriginalFileName(),
                chatFile.getMimeType(),
                chatFile.getFileSize()
        );

        return ApiResponse.ok(response);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        ChatFile chatFile = chatFileService.getFile(fileId);
        Resource resource = chatFileService.downloadFile(fileId);

        String mimeType = chatFile.getMimeType();
        boolean isImage = mimeType != null && mimeType.startsWith("image/");

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream");
        } catch (Exception e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String encodedFileName = URLEncoder.encode(
                chatFile.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(contentType);

        if (isImage) {
            // Images: inline display (no download prompt)
            builder.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName);
        } else {
            // Other files: force download
            builder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
        }

        return builder.body(resource);
    }
}
