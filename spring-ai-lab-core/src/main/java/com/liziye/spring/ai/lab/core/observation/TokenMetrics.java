package com.liziye.spring.ai.lab.core.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 消耗指标 — 按模型维度统计 Token 使用情况。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class TokenMetrics {

    private final Map<String, AtomicLong> tokensByModel = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestsByModel = new ConcurrentHashMap<>();

    /**
     * 记录一次 Token 使用。
     *
     * @param modelName 模型名称
     * @param tokens    消耗的 Token 数
     */
    public void recordUsage(String modelName, long tokens) {
        tokensByModel.computeIfAbsent(modelName, k -> new AtomicLong(0)).addAndGet(tokens);
        requestsByModel.computeIfAbsent(modelName, k -> new AtomicLong(0)).incrementAndGet();
        log.debug("[METRICS-TOKEN] model={} tokens={}", modelName, tokens);
    }

    /**
     * 获取指定模型的 Token 消耗量。
     *
     * @param modelName 模型名称
     * @return Token 总数
     */
    public long getTokensByModel(String modelName) {
        AtomicLong count = tokensByModel.get(modelName);
        return count != null ? count.get() : 0;
    }

    public long getTotalTokens() {
        return tokensByModel.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    /**
     * 获取指定模型的单次请求平均 Token 消耗。
     *
     * @param modelName 模型名称
     * @return 平均 Token 数
     */
    public double getAverageTokensPerRequest(String modelName) {
        long tokens = getTokensByModel(modelName);
        AtomicLong reqAtomic = requestsByModel.get(modelName);
        long requests = reqAtomic != null ? reqAtomic.get() : 0;
        return requests > 0 ? (double) tokens / requests : 0;
    }

    /**
     * 获取 Token 汇总信息。
     *
     * @return 包含总 Token 数和按模型分布的 Map
     */
    public Map<String, Object> getSummary() {
        return Map.of(
                "totalTokens", getTotalTokens(),
                "byModel", Map.copyOf(tokensByModel)
        );
    }
}
