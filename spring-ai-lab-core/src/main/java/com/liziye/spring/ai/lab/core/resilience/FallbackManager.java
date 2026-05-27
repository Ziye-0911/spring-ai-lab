package com.liziye.spring.ai.lab.core.resilience;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 降级管理器 — 多级降级策略。
 *
 * <p>降级链：主模型 → 备用模型 → 缓存结果 → 预设提示。
 *
 * <p>配置方式：
 * <pre>{@code
 * spring.ai.lab.fallback.enabled=true
 * spring.ai.lab.fallback.fallback-model=ollama
 * spring.ai.lab.fallback.fallback-response=抱歉，AI服务暂不可用...
 * }</pre>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class FallbackManager {

    private final boolean enabled;
    private final String fallbackModelName;
    private final String defaultFallbackResponse;

    /** 缓存最近的成功结果（可选特性） */
    private final Map<String, String> resultCache = new ConcurrentHashMap<>();

    public FallbackManager(boolean enabled,
                           String fallbackModelName,
                           String defaultFallbackResponse) {
        this.enabled = enabled;
        this.fallbackModelName = fallbackModelName;
        this.defaultFallbackResponse = defaultFallbackResponse != null
                ? defaultFallbackResponse
                : "抱歉，AI 服务暂时不可用，请稍后重试。";
        log.info("FallbackManager initialized: enabled={}, fallbackModel={}", enabled, fallbackModelName);
    }

    /**
     * 多级降级执行。
     *
     * 降级顺序：
     * 1. 主模型调用
     * 2. 如果失败 → 备用模型（如果配置了）
     * 3. 如果失败 → 缓存结果（如果有）
     * 4. 如果失败 → 默认降级提示
     *
     * @param primaryCall     主模型调用
     * @param fallbackCall    备用模型调用（可为 null）
     * @param cacheKey        缓存 Key（可为 null）
     * @param userPrompt      用户原始输入（用于降级提示上下文）
     * @return 结果（可能来自降级）
     */
    public String executeWithFallback(Supplier<String> primaryCall,
                                       Supplier<String> fallbackCall,
                                       String cacheKey,
                                       String userPrompt) {
        // Level 1: 主模型
        try {
            String result = primaryCall.get();
            if (result != null && !result.isEmpty()) {
                if (cacheKey != null) {
                    resultCache.put(cacheKey, result);
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("[FALLBACK] Primary model failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }

        if (!enabled) {
            throw new RuntimeException("Primary model failed and fallback is disabled");
        }

        // Level 2: 备用模型
        if (fallbackCall != null && fallbackModelName != null) {
            try {
                log.info("[FALLBACK] Switching to fallback model: {}", fallbackModelName);
                String result = fallbackCall.get();
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("[FALLBACK] Fallback model also failed: {}", e.getMessage());
            }
        }

        // Level 3: 缓存结果
        if (cacheKey != null) {
            String cached = resultCache.get(cacheKey);
            if (cached != null) {
                log.info("[FALLBACK] Returning cached result for key: {}", cacheKey);
                return cached;
            }
        }

        // Level 4: 默认降级提示
        log.info("[FALLBACK] Returning default fallback response");
        return defaultFallbackResponse;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
