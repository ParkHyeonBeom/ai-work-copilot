package com.workcopilot.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Google AI Studio (Gemini) REST API 전용 RestClient 설정
 * Spring AI Vertex 스타터 대신 직접 RestClient 사용 (1.0.0-M4 버전 충돌 방지)
 * Reactor Netty 커넥션 풀 간섭 방지를 위해 JdkClientHttpRequestFactory 명시 사용
 */
@Configuration
public class GeminiConfig {

    @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Bean
    public RestClient geminiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(120));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
