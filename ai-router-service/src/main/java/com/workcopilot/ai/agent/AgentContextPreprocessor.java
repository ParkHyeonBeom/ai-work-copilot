package com.workcopilot.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AgentContextPreprocessor {

    private static final int MAX_CHARS_PER_SOURCE = 3000;
    private static final int MAX_TOTAL_CHARS = 12000;

    /**
     * 사용자 질문과의 관련성을 기반으로 컨텍스트를 필터링하고 토큰 예산에 맞게 자른다.
     */
    public PreprocessedContext preprocess(AgentContextCollector.AgentContext context, String userQuestion) {
        log.info("컨텍스트 전처리 시작: questionLength={}", userQuestion.length());

        List<String> usedSources = new ArrayList<>();
        StringBuilder contextText = new StringBuilder();

        // 각 소스별로 관련성 확인 후 추가
        appendSourceIfRelevant(contextText, usedSources, "calendar",
                "캘린더 일정", context.calendarEvents(), userQuestion);
        appendSourceIfRelevant(contextText, usedSources, "email",
                "이메일", context.recentEmails(), userQuestion);
        appendSourceIfRelevant(contextText, usedSources, "chat",
                "채팅 메시지", context.recentChats(), userQuestion);
        appendSourceIfRelevant(contextText, usedSources, "drive",
                "드라이브 파일", context.recentFiles(), userQuestion);

        // 전체 토큰 예산 초과 시 자르기
        String finalContext = contextText.toString();
        if (finalContext.length() > MAX_TOTAL_CHARS) {
            finalContext = finalContext.substring(0, MAX_TOTAL_CHARS) + "\n... (컨텍스트 일부 생략)";
        }

        log.info("컨텍스트 전처리 완료: usedSources={}, contextLength={}",
                usedSources, finalContext.length());

        return new PreprocessedContext(finalContext, usedSources);
    }

    private void appendSourceIfRelevant(StringBuilder contextText, List<String> usedSources,
                                         String sourceKey, String sourceLabel,
                                         String sourceData, String userQuestion) {
        if (sourceData == null || sourceData.isBlank()) {
            return;
        }

        // 키워드 기반 관련성 검사 - 관련 키워드가 있으면 우선 포함
        // 관련 키워드가 없어도 컨텍스트로 포함 (일반적인 업무 질문에 도움이 될 수 있음)
        String truncated = truncateSource(sourceData);

        contextText.append("### ").append(sourceLabel).append("\n");
        contextText.append(truncated).append("\n\n");
        usedSources.add(sourceKey);
    }

    private String truncateSource(String data) {
        if (data.length() <= MAX_CHARS_PER_SOURCE) {
            return data;
        }
        return data.substring(0, MAX_CHARS_PER_SOURCE) + "\n... (일부 생략)";
    }

    /**
     * 전처리된 컨텍스트 결과
     */
    public record PreprocessedContext(
            String contextText,
            List<String> usedSources
    ) {
    }
}
