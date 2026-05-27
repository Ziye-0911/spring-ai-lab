package com.liziye.spring.ai.lab.test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

/**
 * Mock ChatClient — 返回预设回复，不调用真实 AI 接口。
 */
public class MockChatClient {

    private String presetResponse = "这是 Mock AI 的预设回复。";

    public MockChatClient() {
    }

    public MockChatClient(String presetResponse) {
        this.presetResponse = presetResponse;
    }

    public void setPresetResponse(String response) {
        this.presetResponse = response;
    }

    public ChatClient create() {
        ChatModel mockModel = new MockChatModel(presetResponse);
        return ChatClient.builder(mockModel).build();
    }

    private static class MockChatModel implements ChatModel {

        private final String presetResponse;

        MockChatModel(String presetResponse) {
            this.presetResponse = presetResponse;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            AssistantMessage message = new AssistantMessage(presetResponse);
            Generation generation = new Generation(message);
            return new ChatResponse(List.of(generation));
        }
    }
}
