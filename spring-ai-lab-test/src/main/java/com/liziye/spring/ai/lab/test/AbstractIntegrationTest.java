package com.liziye.spring.ai.lab.test;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 集成测试基类 — 用于 ETL 流程测试、工具调用测试等。
 */
public abstract class AbstractIntegrationTest {

    protected Document createTestDocument(String content) {
        Document doc = new Document(content);
        doc.getMetadata().putAll(Map.of(
                "source", "test-source",
                "type", "test"
        ));
        return doc;
    }

    protected List<Document> createTestDocuments(String... contents) {
        return java.util.Arrays.stream(contents)
                .map(this::createTestDocument)
                .toList();
    }
}
