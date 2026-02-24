package com.workcopilot.ai.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SummarizationPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            당신은 문서 요약 전문가입니다.
            주어진 문서 내용을 분석하여 핵심 내용을 간결하게 요약합니다.

            요약 규칙:
            1. 한국어로 작성합니다.
            2. 원문의 핵심 내용을 빠짐없이 포함합니다.
            3. 불필요한 반복이나 부연 설명은 제거합니다.
            4. 요약은 원문 길이의 20~30% 수준으로 작성합니다.

            반드시 다음 JSON 형식으로 응답하세요:
            {
              "summary": "전체 요약 (3~5문장)",
              "keyPoints": ["핵심 포인트 1", "핵심 포인트 2", ...],
              "originalLength": 원문 글자수,
              "summaryLength": 요약 글자수
            }
            """;

    public String build(String content) {
        String prompt = SYSTEM_PROMPT + "\n\n=== 요약할 문서 내용 ===\n" + content;
        log.debug("요약 프롬프트 생성 완료: contentLength={}", content.length());
        return prompt;
    }

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }
}
