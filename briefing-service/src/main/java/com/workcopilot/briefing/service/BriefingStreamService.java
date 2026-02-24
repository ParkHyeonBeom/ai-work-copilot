package com.workcopilot.briefing.service;

import com.workcopilot.briefing.dto.StreamChunk;
import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;
import com.workcopilot.briefing.repository.BriefingRepository;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingStreamService {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분
    private static final int CHUNK_SIZE = 100; // 청크당 문자 수
    private static final long CHUNK_DELAY_MS = 50; // 청크 간 딜레이

    private final BriefingRepository briefingRepository;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter streamBriefing(Long userId, Long briefingId) {
        Briefing briefing = briefingRepository.findById(briefingId)
                .filter(b -> b.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.BRIEFING_NOT_FOUND));

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        if (briefing.getStatus() == BriefingStatus.COMPLETED) {
            streamCompletedBriefing(emitter, briefing);
        } else if (briefing.getStatus() == BriefingStatus.GENERATING) {
            streamGeneratingBriefing(emitter, userId, briefingId);
        } else {
            sendErrorAndComplete(emitter, "브리핑이 아직 생성되지 않았거나 실패한 상태입니다.");
        }

        return emitter;
    }

    private void streamCompletedBriefing(SseEmitter emitter, Briefing briefing) {
        executor.execute(() -> {
            try {
                String content = briefing.getFullContent();
                if (content == null || content.isEmpty()) {
                    content = briefing.getSummary() != null ? briefing.getSummary() : "";
                }

                // 텍스트를 청크로 분할하여 스트리밍
                for (int i = 0; i < content.length(); i += CHUNK_SIZE) {
                    int end = Math.min(i + CHUNK_SIZE, content.length());
                    String chunk = content.substring(i, end);

                    StreamChunk streamChunk = new StreamChunk(chunk, false);
                    emitter.send(SseEmitter.event()
                            .name("briefing")
                            .data(streamChunk));

                    Thread.sleep(CHUNK_DELAY_MS);
                }

                // 완료 이벤트 전송
                emitter.send(SseEmitter.event()
                        .name("briefing")
                        .data(new StreamChunk("", true)));
                emitter.complete();

                log.info("브리핑 스트리밍 완료: briefingId={}", briefing.getId());
            } catch (IOException e) {
                log.warn("SSE 전송 실패: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("SSE 스트리밍 인터럽트: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    private void streamGeneratingBriefing(SseEmitter emitter, Long userId, Long briefingId) {
        executor.execute(() -> {
            try {
                int maxRetries = 60; // 최대 60회 폴링 (5초 간격 = 5분)
                int retryCount = 0;

                while (retryCount < maxRetries) {
                    Briefing briefing = briefingRepository.findById(briefingId)
                            .orElse(null);

                    if (briefing == null) {
                        sendErrorAndComplete(emitter, "브리핑을 찾을 수 없습니다.");
                        return;
                    }

                    if (briefing.getStatus() == BriefingStatus.COMPLETED) {
                        // 완료된 경우 전체 콘텐츠 스트리밍
                        streamCompletedBriefing(emitter, briefing);
                        return;
                    }

                    if (briefing.getStatus() == BriefingStatus.FAILED) {
                        sendErrorAndComplete(emitter, "브리핑 생성에 실패했습니다.");
                        return;
                    }

                    // 진행 중 상태 전송
                    emitter.send(SseEmitter.event()
                            .name("status")
                            .data(new StreamChunk("브리핑 생성 중...", false)));

                    Thread.sleep(5000); // 5초 간격 폴링
                    retryCount++;
                }

                sendErrorAndComplete(emitter, "브리핑 생성 시간이 초과되었습니다.");
            } catch (IOException e) {
                log.warn("SSE 전송 실패: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("SSE 스트리밍 인터럽트: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    private void sendErrorAndComplete(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new StreamChunk(errorMessage, true)));
            emitter.complete();
        } catch (IOException e) {
            log.warn("에러 이벤트 전송 실패: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
