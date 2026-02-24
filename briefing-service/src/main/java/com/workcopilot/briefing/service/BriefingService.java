package com.workcopilot.briefing.service;

import com.workcopilot.briefing.client.AiRouterClient;
import com.workcopilot.briefing.client.IntegrationClient;
import com.workcopilot.briefing.dto.*;
import com.workcopilot.briefing.entity.Briefing;
import com.workcopilot.briefing.entity.BriefingStatus;
import com.workcopilot.briefing.repository.BriefingRepository;
import com.workcopilot.common.exception.BusinessException;
import com.workcopilot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingService {

    private final BriefingRepository briefingRepository;
    private final IntegrationClient integrationClient;
    private final AiRouterClient aiRouterClient;

    @Transactional
    public BriefingResponse generateDailyBriefing(Long userId) {
        log.info("일일 브리핑 생성 시작: userId={}", userId);

        LocalDate today = LocalDate.now();

        // 1. 오늘 이미 완료된 브리핑이 있으면 반환
        Optional<Briefing> existing = briefingRepository.findByUserIdAndBriefingDate(userId, today);
        if (existing.isPresent() && existing.get().getStatus() == BriefingStatus.COMPLETED) {
            log.info("이미 완료된 브리핑 존재: userId={}, briefingId={}", userId, existing.get().getId());
            return BriefingResponse.from(existing.get());
        }

        // 2. 새 브리핑 엔티티 생성 (기존 PENDING/FAILED가 있으면 재사용)
        Briefing briefing = existing.orElseGet(() -> briefingRepository.save(
                Briefing.builder()
                        .userId(userId)
                        .briefingDate(today)
                        .status(BriefingStatus.PENDING)
                        .build()
        ));

        try {
            // 3. GENERATING 상태로 변경
            briefing.updateStatus(BriefingStatus.GENERATING);
            briefingRepository.save(briefing);

            // 4. integration-service에서 업무 데이터 수집
            WorkDataDto workData = integrationClient.collectWorkData();

            // 5. ai-router-service로 브리핑 생성 요청
            BriefingRequest aiRequest = new BriefingRequest(
                    userId,
                    workData.events(),
                    workData.emails(),
                    workData.files()
            );
            BriefingAiResponse aiResponse = aiRouterClient.generateBriefing(aiRequest);

            // 6. 브리핑 엔티티 업데이트
            briefing.complete(
                    aiResponse.summary(),
                    aiResponse.fullContent(),
                    aiResponse.keyPoints(),
                    aiResponse.actionItems(),
                    workData.events() != null ? workData.events().size() : 0,
                    workData.emails() != null ? workData.emails().size() : 0,
                    workData.files() != null ? workData.files().size() : 0
            );
            briefingRepository.save(briefing);

            log.info("일일 브리핑 생성 완료: userId={}, briefingId={}", userId, briefing.getId());
            return BriefingResponse.from(briefing);

        } catch (Exception e) {
            // 8. 실패 시 FAILED 상태로 변경
            log.error("일일 브리핑 생성 실패: userId={}, error={}", userId, e.getMessage(), e);
            briefing.fail("브리핑 생성 중 오류가 발생했습니다: " + e.getMessage());
            briefingRepository.save(briefing);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "브리핑 생성 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BriefingResponse getBriefing(Long userId, Long briefingId) {
        Briefing briefing = briefingRepository.findById(briefingId)
                .filter(b -> b.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.BRIEFING_NOT_FOUND));

        return BriefingResponse.from(briefing);
    }

    @Transactional(readOnly = true)
    public List<BriefingListResponse> getBriefingHistory(Long userId) {
        return briefingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BriefingListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<BriefingResponse> getTodayBriefing(Long userId) {
        return briefingRepository.findByUserIdAndBriefingDate(userId, LocalDate.now())
                .map(BriefingResponse::from);
    }
}
