package com.liziye.spring.ai.lab.core.advisor;

import com.liziye.spring.ai.lab.core.model.AgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Function;

/**
 * 降级增强器 — 模型调用失败时的降级策略。
 *
 * <p>支持三种降级方式：
 * <ol>
 *   <li>切换到备用模型</li>
 *   <li>返回缓存结果</li>
 *   <li>返回预设提示</li>
 * </ol>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class FallbackAdvisor {

    /** 默认降级消息 */
    private final String defaultFallbackMessage;

    /** 自定义降级处理器 */
    private final Function<String, AgentResponse> customFallbackHandler;

    /**
     * 使用默认降级消息构造。
     *
     * @param defaultFallbackMessage 默认降级消息
     */
    public FallbackAdvisor(String defaultFallbackMessage) {
        this(defaultFallbackMessage, null);
    }

    /**
     * 使用自定义降级处理器构造。
     *
     * @param defaultFallbackMessage 默认降级消息
     * @param customFallbackHandler  自定义降级处理器，可为 {@code null}
     */
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
     * <p>优先调用自定义处理器，若失败或无自定义处理器则返回预设降级消息。
     *
     * @param userInput 用户原始输入
     * @param error     触发降级的异常
     * @return 降级后的 {@link AgentResponse}
     */
    public AgentResponse fallback(String userInput, Throwable error) {
        log.warn("[FALLBACK] Triggered due to: {}, using fallback response", error.getMessage());

        if (customFallbackHandler != null) {
            try {
                return customFallbackHandler.apply(userInput);
            } catch (Exception e) {
                log.error("[FALLBACK] Custom handler failed: {}", e.getMessage());
            }
        }

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
