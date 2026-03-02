package com.workcopilot.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.dto.AiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LlmRouterTest {

    @Mock
    private ChatModel openAiChatModel;

    @Mock
    private ChatModel anthropicChatModel;

    @Mock
    private ChatModel ollamaChatModel;

    @Mock
    private RestClient ollamaRestClient;

    @Mock
    private RestClient geminiRestClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("route_classify타입_Ollama비활성화시_mock응답반환")
    void route_classify타입_Ollama비활성화시_mock응답반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");

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
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");

        // when
        AiResponse response = router.route("keyword", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
        assertThat(response.result()).contains("keywords");
    }

    @Test
    @DisplayName("route_briefing타입_모두비활성화시_mock응답반환")
    void route_briefing타입_모두비활성화시_mock응답반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");

        // when
        AiResponse response = router.route("briefing", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
    }

    @Test
    @DisplayName("route_summarize타입_모두비활성화시_mock응답반환")
    void route_summarize타입_모두비활성화시_mock응답반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");

        // when
        AiResponse response = router.route("summarize", "테스트 프롬프트");

        // then
        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
    }

    @Test
    @DisplayName("getModelForTask_classify_Ollama활성화시_올라마모델반환")
    void getModelForTask_classify_Ollama활성화시_올라마모델반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, true, false, "", "gemini-2.5-flash");

        // when
        String model = router.getModelForTask("classify");

        // then
        assertThat(model).isEqualTo("ollama-llama3.1-8b");
    }

    @Test
    @DisplayName("getModelForTask_classify_Ollama비활성화시_mock모드반환")
    void getModelForTask_classify_Ollama비활성화시_mock모드반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");

        // when
        String model = router.getModelForTask("classify");

        // then
        assertThat(model).isEqualTo("ollama-llama3.1-8b (mock)");
    }

    @Test
    @DisplayName("getModelForTask_briefing_Gemini비활성화_Ollama비활성화시_mock반환")
    void getModelForTask_briefing_Gemini비활성화_Ollama비활성화시_mock반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, true, false, false, "", "gemini-2.5-flash");

        // when
        String model = router.getModelForTask("briefing");

        // then
        assertThat(model).isEqualTo("mock");
    }

    @Test
    @DisplayName("getModelForTask_briefing_모두비활성화시_mock반환")
    void getModelForTask_briefing_모두비활성화시_mock반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");

        // when
        String model = router.getModelForTask("briefing");

        // then
        assertThat(model).isEqualTo("mock");
    }

    @Test
    @DisplayName("getModelForTask_summarize_Ollama활성화시_Ollama반환")
    void getModelForTask_summarize_Ollama활성화시_Ollama반환() {
        // given
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, true, true, false, "", "gemini-2.5-flash");

        // when
        String model = router.getModelForTask("summarize");

        // then
        assertThat(model).isEqualTo("ollama-llama3.1-8b");
    }

    @Test
    @DisplayName("route_briefing타입_Gemini비활성화시_Ollama비활성화시_mock응답반환")
    void route_briefing타입_Gemini비활성화시_Ollama비활성화시_mock응답반환() {
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, false, false, "", "gemini-2.5-flash");
        AiResponse response = router.route("briefing", "테스트 프롬프트");
        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
    }

    @Test
    @DisplayName("getModelForTask_briefing_Gemini활성화시_Gemini모델반환")
    void getModelForTask_briefing_Gemini활성화시_Gemini모델반환() {
        LlmRouter router = new LlmRouter(openAiChatModel, anthropicChatModel, ollamaChatModel, ollamaRestClient, geminiRestClient, objectMapper, false, true, true, "test-api-key", "gemini-2.5-flash");
        String model = router.getModelForTask("briefing");
        assertThat(model).isEqualTo("gemini-2.5-flash");
    }
}
