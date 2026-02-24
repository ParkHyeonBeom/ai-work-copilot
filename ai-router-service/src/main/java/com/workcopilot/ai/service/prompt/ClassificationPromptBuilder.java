package com.workcopilot.ai.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClassificationPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            당신은 이메일 분류 전문가입니다.
            주어진 이메일 내용을 분석하여 중요도와 카테고리를 분류합니다.

            분류 기준:
            - 중요도(importance): high, medium, low
            - 카테고리(category): urgent(긴급), meeting(회의), report(보고), request(요청), notification(알림), general(일반)
            - 신뢰도(confidence): 0.0 ~ 1.0

            반드시 다음 JSON 형식으로 응답하세요:
            {
              "importance": "high|medium|low",
              "category": "카테고리",
              "confidence": 0.95,
              "reason": "분류 이유 한 줄 설명"
            }
            """;

    public String build(String content) {
        String prompt = SYSTEM_PROMPT + "\n\n=== 분류할 이메일 내용 ===\n" + content;
        log.debug("분류 프롬프트 생성 완료: contentLength={}", content.length());
        return prompt;
    }

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
