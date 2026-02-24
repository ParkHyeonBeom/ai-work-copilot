package com.workcopilot.ai.service.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 프로덕션 프로파일용 Milvus 벡터 스토어 구현체.
 * TODO: Milvus SDK를 사용한 실제 벡터 저장/검색 구현
 */
@Slf4j
@Service
@Profile("prod")
public class MilvusVectorStoreService implements VectorStoreService {

    // TODO: Milvus 클라이언트 주입
    // private final MilvusServiceClient milvusClient;

    @Override
    public void store(String documentId, String content, Map<String, String> metadata) {
        // TODO: Milvus에 임베딩 벡터 저장 구현
        // 1. EmbeddingService를 통해 content를 벡터로 변환
        // 2. Milvus 컬렉션에 벡터 + 메타데이터 저장
        log.warn("MilvusVectorStoreService.store() 미구현 - documentId={}", documentId);
        throw new UnsupportedOperationException("Milvus 벡터 스토어 미구현");
    }

    @Override
    public List<SearchResult> search(String query, int topK) {
        // TODO: Milvus에서 유사도 검색 구현
        // 1. EmbeddingService를 통해 query를 벡터로 변환
        // 2. Milvus에서 ANN 검색 수행
        // 3. SearchResult 목록으로 변환하여 반환
        log.warn("MilvusVectorStoreService.search() 미구현 - query='{}'", query);
        throw new UnsupportedOperationException("Milvus 벡터 스토어 미구현");
    }
}
