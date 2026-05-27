package com.liziye.spring.ai.lab.test;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock VectorStore — 内存中模拟向量存储，返回预设文档列表。
 */
public class MockVectorStore implements VectorStore {

    private final List<Document> documents = new ArrayList<>();
    private final Map<String, Document> docMap = new ConcurrentHashMap<>();
    private List<Document> presetResults = Collections.emptyList();

    public void setPresetResults(List<Document> results) {
        this.presetResults = results != null ? new ArrayList<>(results) : Collections.emptyList();
    }

    public void addPresetDocument(Document doc) {
        this.documents.add(doc);
    }

    @Override
    public void add(List<Document> documents) {
        this.documents.addAll(documents);
        for (Document doc : documents) {
            docMap.put(doc.getId(), doc);
        }
    }

    @Override
    public void delete(List<String> idList) {
        idList.forEach(docMap::remove);
        documents.removeIf(d -> idList.contains(d.getId()));
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        // Mock 实现：清空所有数据（简化处理）
        documents.clear();
        docMap.clear();
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (!presetResults.isEmpty()) {
            int topK = request.getTopK();
            if (topK >= presetResults.size()) {
                return new ArrayList<>(presetResults);
            }
            return new ArrayList<>(presetResults.subList(0, topK));
        }
        return List.of();
    }

    @Override
    public List<Document> similaritySearch(String query) {
        return similaritySearch(SearchRequest.builder().query(query).topK(5).build());
    }

    public List<Document> getStoredDocuments() {
        return Collections.unmodifiableList(documents);
    }

    public void clear() {
        documents.clear();
        docMap.clear();
        presetResults.clear();
    }
}
