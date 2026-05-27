package com.liziye.spring.ai.lab.core.advisor;

import com.liziye.spring.ai.lab.core.model.AgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Function;

/**
 * 降级增强器 — 模型调用失败时的降级策略。
 *
 * 支持三种降级方式：
 * 1. 切换到备用模型
 * 2. 返回缓存结果
 * 3. 返回预设提示
 */
@Slf4j
public class FallbackAdvisor {

    /** 降级时默认返回的消息 */
    private final String defaultFallbackMessage;

    /** 自定义降级处理器 */
    private final Function<String, AgentResponse> customFallbackHandler;

    public FallbackAdvisor(String defaultFallbackMessage) {
        this(defaultFallbackMessage, null);
    }

    public FallbackAdvisor(String defaultFallbackMessage,
                           Function<String, AgentResponse> customFallbackHandler) {
        this.defaultFallbackMessage = defaultFallbackMessage != null
                ? defaultFallbackMessage
                : "抱歉，AI 服务暂时不可用，请稍后重试。";
        this.customFallbackHandler = customFallbackHandler;
    }

    /**
     * 执行降级逻辑。
     *
     * @param userInput 原始用户输入
     * @param error     导致降级的异常
     * @return 降级响应
     */
    public AgentResponse fallback(String userInput, Throwable error) {
        log.warn("[FALLBACK] Triggered due to: {}, using fallback response", error.getMessage());

        // 1. 自定义降级处理器（最高优先级）
        if (customFallbackHandler != null) {
            try {
                return customFallbackHandler.apply(userInput);
            } catch (Exception e) {
                log.error("[FALLBACK] Custom handler failed: {}", e.getMessage());
            }
        }

        // 2. 默认降级响应
        return AgentResponse.builder()
                .content(defaultFallbackMessage)
                .fallback(true)
                .metadata(Map.of(
                        "fallback", true,
                        "fallback_reason", error.getMessage() != null
                                ? error.getMessage() : "unknown"))
                .build();
    }
}
