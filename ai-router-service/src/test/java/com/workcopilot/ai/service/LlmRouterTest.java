package com.workcopilot.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workcopilot.ai.dto.AiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LlmRouterTest {

    @Mock
    private RestClient geminiRestClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("route_classify타입_API키없을때_mock응답반환")
    void route_classify타입_API키없을때_mock응답반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        AiResponse response = router.route("classify", "테스트 프롬프트");

        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
        assertThat(response.result()).contains("importance");
    }

    @Test
    @DisplayName("route_keyword타입_API키없을때_mock응답반환")
    void route_keyword타입_API키없을때_mock응답반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        AiResponse response = router.route("keyword", "테스트 프롬프트");

        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
        assertThat(response.result()).contains("keywords");
    }

    @Test
    @DisplayName("route_briefing타입_API키없을때_mock응답반환")
    void route_briefing타입_API키없을때_mock응답반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        AiResponse response = router.route("briefing", "테스트 프롬프트");

        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
    }

    @Test
    @DisplayName("route_summarize타입_API키없을때_mock응답반환")
    void route_summarize타입_API키없을때_mock응답반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        AiResponse response = router.route("summarize", "테스트 프롬프트");

        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
    }

    @Test
    @DisplayName("route_agent타입_API키없을때_mock응답반환")
    void route_agent타입_API키없을때_mock응답반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        AiResponse response = router.route("agent", "테스트 프롬프트");

        assertThat(response).isNotNull();
        assertThat(response.model()).contains("mock");
        assertThat(response.result()).contains("AI 어시스턴트");
    }

    @Test
    @DisplayName("getModelForTask_API키있을때_Gemini모델반환")
    void getModelForTask_API키있을때_Gemini모델반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "test-api-key", "gemini-2.5-flash");

        String model = router.getModelForTask("briefing");

        assertThat(model).isEqualTo("gemini-2.5-flash");
    }

    @Test
    @DisplayName("getModelForTask_API키없을때_mock반환")
    void getModelForTask_API키없을때_mock반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        String model = router.getModelForTask("briefing");

        assertThat(model).isEqualTo("gemini-2.5-flash (mock)");
    }

    @Test
    @DisplayName("createMockResponse_각_taskType별_적절한응답반환")
    void createMockResponse_각_taskType별_적절한응답반환() {
        LlmRouter router = new LlmRouter(geminiRestClient, objectMapper, "", "gemini-2.5-flash");

        assertThat(router.createMockResponse("classify", "mock").result()).contains("importance");
        assertThat(router.createMockResponse("keyword", "mock").result()).contains("keywords");
        assertThat(router.createMockResponse("briefing", "mock").result()).contains("브리핑");
        assertThat(router.createMockResponse("summarize", "mock").result()).contains("요약");
        assertThat(router.createMockResponse("agent", "mock").result()).contains("어시스턴트");
        assertThat(router.createMockResponse("unknown", "mock").result()).contains("처리 결과");
    }
}
