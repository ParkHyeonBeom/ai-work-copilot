package com.workcopilot.ai.service;

import com.workcopilot.ai.dto.AiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LlmRouterTest {

    @Mock
    private ChatModel openAiChatModel;

    @Mock
    private ChatModel ollamaChatModel;

    @Test
    @DisplayName("route_classify타입_Ollama비활성화시_mock응답반환")
    void route_classify타입_Ollama비활성화시_mock응답반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, false);

        // when
        AiResponse response = router.route("classify", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
        assertThat(response.result()).contains("importance");
    }

    @Test
    @DisplayName("route_keyword타입_Ollama비활성화시_mock응답반환")
    void route_keyword타입_Ollama비활성화시_mock응답반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, false);

        // when
        AiResponse response = router.route("keyword", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
        assertThat(response.result()).contains("keywords");
    }

    @Test
    @DisplayName("route_briefing타입_OpenAI라우팅_정상응답")
    void route_briefing타입_OpenAI라우팅_정상응답() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, false);

        // when
        AiResponse response = router.route("briefing", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("route_summarize타입_OpenAI라우팅_정상응답")
    void route_summarize타입_OpenAI라우팅_정상응답() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, false);

        // when
        AiResponse response = router.route("summarize", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("getModelForTask_classify_Ollama활성화시_올라마모델반환")
    void getModelForTask_classify_Ollama활성화시_올라마모델반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, true);

        // when
        String model = router.getModelForTask("classify");

        // then
        assertThat(model).isEqualTo("ollama-llama3.1-8b");
    }

    @Test
    @DisplayName("getModelForTask_classify_Ollama비활성화시_GPT4o반환")
    void getModelForTask_classify_Ollama비활성화시_GPT4o반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, false);

        // when
        String model = router.getModelForTask("classify");

        // then
        assertThat(model).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("getModelForTask_briefing_항상GPT4o반환")
    void getModelForTask_briefing_항상GPT4o반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, true);

        // when
        String model = router.getModelForTask("briefing");

        // then
        assertThat(model).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("getModelForTask_summarize_항상GPT4o반환")
    void getModelForTask_summarize_항상GPT4o반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, ollamaChatModel, false);

        // when
        String model = router.getModelForTask("summarize");

        // then
        assertThat(model).isEqualTo("gpt-4o");
    }
}
