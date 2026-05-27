package com.liziye.spring.ai.lab.test;

import com.liziye.spring.ai.lab.core.model.AgentContext;

/**
 * Agent 单元测试基类 — 自动注入 Mock 组件。
 *
 * @param <T> 场景 Context 类型
 */
public abstract class AbstractAgentTest<T extends AgentContext> {

    protected final MockChatClient mockChatClient = new MockChatClient();
    protected final MockVectorStore mockVectorStore = new MockVectorStore();
    protected final MockEmbeddingModel mockEmbeddingModel = new MockEmbeddingModel();
    protected final MockConversationMemory mockMemory = new MockConversationMemory();

    protected void givenAiResponse(String response) {
        mockChatClient.setPresetResponse(response);
    }

    protected void givenRetrievedDocuments(java.util.List<org.springframework.ai.document.Document> docs) {
        mockVectorStore.setPresetResults(docs);
    }

    protected abstract T createContext();

    protected void setUp() {
        mockMemory.clearAll();
        mockVectorStore.clear();
        mockChatClient.setPresetResponse("Mock response");
    }
}
