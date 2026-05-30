package com.liziye.spring.ai.lab.core.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.liziye.spring.ai.lab.core.config.DashScopeProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容 {@link ChatModel} 实现。
 *
 * <p>通过 REST API 调用 OpenAI 兼容接口（如 DashScope、自建代理等）。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class DashScopeChatModel implements ChatModel {

    private final WebClient webClient;
    private final DashScopeProperties properties;
    private static final com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 构造 DashScope ChatModel。
     *
     * <p>根据配置属性初始化 {@link WebClient} 和 HTTP 客户端。
     *
     * @param properties DashScope API 配置属性
     */
    public DashScopeChatModel(DashScopeProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(properties.getReadTimeout()))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        properties.getConnectTimeout() * 1000);

        this.webClient = WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        log.info("DashScopeChatModel initialized: baseUrl={}, model={}", properties.getBaseUrl(), properties.getModel());
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        OpenAIRequest request = buildRequest(prompt, false);
        try {
            String rawResponse = webClient.post()
                    .uri(properties.getBaseUrl())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(properties.getReadTimeout()));

            log.info("[LLM] raw_response_preview={}", truncate(rawResponse, 500));

            // 先尝试解析为通用错误响应 {"code":..., "msg":..., "ok":...}
            try {
                ApiErrorResponse errorResp = mapper.readValue(rawResponse, ApiErrorResponse.class);
                if (errorResp.getCode() != null && errorResp.getCode() != 200 && !errorResp.isOk()) {
                    String errMsg = String.format("API error [code=%d]: %s (using model=%s)",
                            errorResp.getCode(), errorResp.getMsg(), properties.getModel());
                    log.error("[LLM] {}", errMsg);
                    throw new RuntimeException(errMsg);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                // 不是错误响应格式，继续尝试 OpenAI 格式
            }

            // 解析为 OpenAI 格式
            OpenAIResponse response = mapper.readValue(rawResponse, OpenAIResponse.class);
            log.info("[LLM] parsed choices={}", response.getChoices() != null ? response.getChoices().size() : 0);

            return toChatResponse(response);
        } catch (RuntimeException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("OpenAI-compatible API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        OpenAIRequest request = buildRequest(prompt, true);

        return webClient.post()
                .uri(properties.getBaseUrl())
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(data -> !data.equals("[DONE]"))
                .map(this::parseStreamEvent)
                .filter(Objects::nonNull)
                .map(this::toChatResponse);
    }

    private OpenAIRequest buildRequest(Prompt prompt, boolean stream) {
        List<Message> promptMessages = prompt.getInstructions();

        List<OpenAIMessage> messages = promptMessages.stream()
                .map(msg -> {
                    String role = msg.getMessageType().name().toLowerCase();
                    return new OpenAIMessage(role, msg.getText());
                })
                .collect(Collectors.toList());

        return OpenAIRequest.builder()
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .temperature(properties.getTemperature())
                .messages(messages)
                .stream(stream)
                .build();
    }

    private ChatResponse toChatResponse(OpenAIResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return new ChatResponse(List.of());
        }

        OpenAIChoice choice = response.getChoices().get(0);
        String text;

        // 流式：delta.content，非流式：message.content
        if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
            text = choice.getDelta().getContent();
        } else if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
            text = choice.getMessage().getContent();
        } else {
            text = "";
        }

        AssistantMessage assistantMessage = new AssistantMessage(text);
        Generation generation = new Generation(assistantMessage);

        return new ChatResponse(List.of(generation));
    }

    private OpenAIResponse parseStreamEvent(String json) {
        try {
            return mapper.readValue(json, OpenAIResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse stream event: {}", json, e);
            return null;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }

    // ========== API 通用错误响应 ==========

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApiErrorResponse {
        private Integer code;
        private String msg;
        private boolean ok;
        private Object data;
    }

    // ========== OpenAI 兼容数据类 ==========

    @Data
    @Builder
    static class OpenAIRequest {
        private String model;
        @JsonProperty("max_tokens")
        private int maxTokens;
        private double temperature;
        private List<OpenAIMessage> messages;
        private boolean stream;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAIMessage {
        private String role;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAIResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<OpenAIChoice> choices;
        private OpenAIUsage usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAIChoice {
        private int index;
        private OpenAIMessage message;
        private OpenAIMessage delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAIUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
