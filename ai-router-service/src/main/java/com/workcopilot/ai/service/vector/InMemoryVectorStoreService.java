package com.workcopilot.ai.service.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 로컬 프로파일용 인메모리 벡터 스토어 구현체.
 * 단순 키워드 매칭 기반으로 유사도를 계산한다 (임베딩 없이 동작).
 */
@Slf4j
@Service
@Profile({"local", "k8s"})
public class InMemoryVectorStoreService implements VectorStoreService {

    private final Map<String, DocumentEntry> store = new ConcurrentHashMap<>();

    @Override
    public void store(String documentId, String content, Map<String, String> metadata) {
        store.put(documentId, new DocumentEntry(documentId, content, metadata));
        log.info("인메모리 벡터 스토어에 문서 저장: documentId={}, contentLength={}",
                documentId, content.length());
    }

    @Override
    public List<SearchResult> search(String query, int topK) {
        if (store.isEmpty()) {
            log.debug("벡터 스토어가 비어있음 - 빈 결과 반환");
            return List.of();
        }

        String[] queryTokens = query.toLowerCase().split("\\s+");

        List<SearchResult> results = store.values().stream()
                .map(entry -> {
                    double score = calculateSimpleSimilarity(queryTokens, entry.content().toLowerCase());
                    return new SearchResult(entry.documentId(), entry.content(), score, entry.metadata());
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .filter(r -> r.score() > 0.0)
                .collect(Collectors.toList());

        log.debug("인메모리 검색 완료: query='{}', topK={}, results={}", query, topK, results.size());
        return results;
    }

    /**
     * 단순 토큰 매칭 기반 유사도 계산 (로컬 테스트용)
     */
    private double calculateSimpleSimilarity(String[] queryTokens, String content) {
        long matchCount = Arrays.stream(queryTokens)
                .filter(content::contains)
                .count();
        return queryTokens.length > 0 ? (double) matchCount / queryTokens.length : 0.0;
    }

    private record DocumentEntry(
            String documentId,
            String content,
            Map<String, String> metadata
    ) {
    }
}
