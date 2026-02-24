package com.workcopilot.ai.service.vector;

import java.util.List;
import java.util.Map;

public interface VectorStoreService {

    /**
     * 문서를 벡터 스토어에 저장한다.
     *
     * @param documentId 문서 고유 ID
     * @param content    문서 내용
     * @param metadata   메타데이터 (documentType, source 등)
     */
    void store(String documentId, String content, Map<String, String> metadata);

    /**
     * 유사도 검색을 수행한다.
     *
     * @param query 검색 쿼리
     * @param topK  반환할 결과 수
     * @return 유사도 순으로 정렬된 검색 결과 목록
     */
    List<SearchResult> search(String query, int topK);
}
