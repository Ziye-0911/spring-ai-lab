package com.liziye.spring.ai.lab.test;

import com.liziye.spring.ai.lab.core.model.Message;
import com.liziye.spring.ai.lab.core.model.AgentRequest;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.AgentContext;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 测试数据工厂 — 提供常用的预设数据。
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static Message userMessage(String content) {
        return Message.user(content);
    }

    public static Message assistantMessage(String content) {
        return Message.assistant(content);
    }

    public static AgentRequest simpleRequest(String userInput) {
        return AgentRequest.builder()
                .conversationId("test-conv-001")
                .userInput(userInput)
                .build();
    }

    public static AgentResponse simpleResponse(String content) {
        return AgentResponse.builder()
                .content(content)
                .conversationId("test-conv-001")
                .metadata(Map.of("model", "test-model", "tokens", 100L))
                .build();
    }

    public static Document textDocument(String content) {
        Document doc = new Document(content);
        doc.getMetadata().put("source", "test-fixture");
        doc.getMetadata().put("type", "text");
        return doc;
    }

    public static List<Document> textDocuments(String... contents) {
        return java.util.Arrays.stream(contents)
                .map(TestFixtures::textDocument)
                .toList();
    }

    public static AgentContext simpleContext() {
        AgentContext context = new AgentContext();
        context.setConversationId("test-conv-001");
        context.setModelProvider("test-model");
        return context;
    }
}
