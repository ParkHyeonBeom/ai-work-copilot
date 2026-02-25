package com.workcopilot.integration.service;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.workcopilot.common.audit.AuditAction;
import com.workcopilot.common.audit.Audited;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import com.workcopilot.integration.dto.DriveFileDto;
import com.workcopilot.integration.google.GoogleCredentialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveService {

    private static final Set<String> EXPORTABLE_MIME_TYPES = Set.of(
            "application/vnd.google-apps.document",
            "application/vnd.google-apps.spreadsheet",
            "application/vnd.google-apps.presentation"
    );

    private static final Set<String> TEXT_MIME_TYPES = Set.of(
            "text/plain",
            "text/html",
            "text/csv",
            "application/json",
            "application/xml"
    );

    private final GoogleCredentialProvider credentialProvider;
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;
    @Qualifier("googleApplicationName")
    private final String applicationName;

    @Audited(action = AuditAction.DRIVE_ACCESSED)
    public List<DriveFileDto> getRecentFiles(Long userId, int maxResults) {
        log.info("최근 파일 조회: userId={}, maxResults={}", userId, maxResults);

        try {
            Drive drive = buildDriveService(userId);

            FileList fileList = drive.files().list()
                    .setPageSize(maxResults)
                    .setOrderBy("modifiedTime desc")
                    .setFields("files(id, name, mimeType, modifiedTime, owners, webViewLink)")
                    .setQ("trashed = false")
                    .execute();

            List<File> files = fileList.getFiles();
            if (files == null || files.isEmpty()) {
                log.info("조회된 파일 없음: userId={}", userId);
                return Collections.emptyList();
            }

            List<DriveFileDto> result = files.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.info("파일 조회 완료: userId={}, count={}", userId, result.size());
            return result;

        } catch (IOException e) {
            log.error("Google Drive API 호출 실패: userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "파일 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    public String getFileContent(Long userId, String fileId) {
        log.info("파일 콘텐츠 조회: userId={}, fileId={}", userId, fileId);

        try {
            Drive drive = buildDriveService(userId);

            File file = drive.files().get(fileId)
                    .setFields("id, name, mimeType")
                    .execute();

            String mimeType = file.getMimeType();
            String content;

            if (EXPORTABLE_MIME_TYPES.contains(mimeType)) {
                content = exportGoogleDoc(drive, fileId, mimeType);
            } else if (TEXT_MIME_TYPES.contains(mimeType)) {
                content = downloadTextFile(drive, fileId);
            } else {
                log.warn("텍스트 추출 불가능한 파일 형식: mimeType={}", mimeType);
                content = "[텍스트 추출 불가: " + mimeType + "]";
            }

            log.info("파일 콘텐츠 조회 완료: userId={}, fileId={}, contentLength={}",
                    userId, fileId, content.length());
            return content;

        } catch (IOException e) {
            log.error("Google Drive API 호출 실패: userId={}, fileId={}, error={}",
                    userId, fileId, e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR,
                    "파일 콘텐츠 조회에 실패했습니다: " + e.getMessage());
        }
    }

    private Drive buildDriveService(Long userId) {
        return new Drive.Builder(httpTransport, jsonFactory, credentialProvider.getCredential(userId))
                .setApplicationName(applicationName)
                .build();
    }

    private String exportGoogleDoc(Drive drive, String fileId, String mimeType) throws IOException {
        String exportMimeType = switch (mimeType) {
            case "application/vnd.google-apps.document" -> "text/plain";
            case "application/vnd.google-apps.spreadsheet" -> "text/csv";
            case "application/vnd.google-apps.presentation" -> "text/plain";
            default -> "text/plain";
        };

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        drive.files().export(fileId, exportMimeType)
                .executeMediaAndDownloadTo(outputStream);

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private String downloadTextFile(Drive drive, String fileId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        drive.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private DriveFileDto convertToDto(File file) {
        LocalDateTime modifiedTime = null;
        if (file.getModifiedTime() != null) {
            modifiedTime = Instant.ofEpochMilli(file.getModifiedTime().getValue())
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime();
        }

        List<String> owners = Collections.emptyList();
        if (file.getOwners() != null) {
            owners = file.getOwners().stream()
                    .map(owner -> owner.getDisplayName() != null
                            ? owner.getDisplayName()
                            : owner.getEmailAddress())
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toList());
        }

        return new DriveFileDto(
                file.getId(),
                file.getName(),
                file.getMimeType(),
                modifiedTime,
                owners,
                file.getWebViewLink()
        );
    }
}
