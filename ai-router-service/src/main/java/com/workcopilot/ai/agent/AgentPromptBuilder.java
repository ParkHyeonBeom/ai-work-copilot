package com.workcopilot.ai.agent;

import com.workcopilot.ai.entity.ConversationMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentPromptBuilder {

    /**
     * 시스템 프롬프트 + 컨텍스트 + 대화 히스토리 + 사용자 메시지를 조합하여
     * Gemini에 전달할 최종 프롬프트를 생성한다.
     */
    public String buildPrompt(String userMessage, String preprocessedContext,
                               List<ConversationMessage> recentHistory) {
        StringBuilder prompt = new StringBuilder();

        // 시스템 프롬프트
        prompt.append("당신은 AI Work Copilot 업무 어시스턴트입니다. 사용자의 업무 관련 질문에 답변해주세요.\n\n");

        // 업무 컨텍스트
        if (preprocessedContext != null && !preprocessedContext.isBlank()) {
            prompt.append("## 현재 업무 컨텍스트\n");
            prompt.append(preprocessedContext).append("\n\n");
        }

        // 대화 히스토리
        if (recentHistory != null && !recentHistory.isEmpty()) {
            prompt.append("## 대화 히스토리\n");
            // recentHistory는 최신순으로 정렬되어 있으므로 역순으로 출력 (오래된 것부터)
            for (int i = recentHistory.size() - 1; i >= 0; i--) {
                ConversationMessage msg = recentHistory.get(i);
                prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        // 사용자 질문
        prompt.append("## 사용자 질문\n");
        prompt.append(userMessage).append("\n\n");

        // 지시사항
        prompt.append("한국어로 답변해주세요. 업무 컨텍스트를 활용하여 구체적이고 실용적인 답변을 제공하세요.");

        return prompt.toString();
    }
}
