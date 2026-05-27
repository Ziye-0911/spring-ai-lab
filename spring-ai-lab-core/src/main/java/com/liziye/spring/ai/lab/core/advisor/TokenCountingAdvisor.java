package com.liziye.spring.ai.lab.core.advisor;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Token 统计增强器 — 统计每次对话的 Token 消耗。
 *
 * <p>提供全局级别的 Token 计数和平均消耗统计。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class TokenCountingAdvisor {

    /** 总 Token 消耗 */
    private final AtomicLong totalTokens = new AtomicLong(0);

    /** 总请求次数 */
    private final AtomicLong totalRequests = new AtomicLong(0);

    /**
     * 记录一次调用的 Token 消耗。
     *
     * @param modelName   模型名称
     * @param inputTokens 输入 Token 数
     * @param outputTokens 输出 Token 数
     */
    public void recordUsage(String modelName, long inputTokens, long outputTokens) {
        totalTokens.addAndGet(inputTokens + outputTokens);
        totalRequests.incrementAndGet();
        log.debug("[TOKEN] model={} input={} output={} total_session={}",
                modelName, inputTokens, outputTokens, inputTokens + outputTokens);
    }

    /**
     * 获取全局总 Token 消耗。
     *
     * @return 累计 Token 总数
     */
    public long getTotalTokens() {
        return totalTokens.get();
    }

    /**
     * 获取全局总请求次数。
     *
     * @return 累计请求次数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * 计算每次请求的平均 Token 消耗。
     *
     * @return 平均 Token 数，无请求时返回 0
     */
    public double getAverageTokensPerRequest() {
        long requests = totalRequests.get();
        return requests > 0 ? (double) totalTokens.get() / requests : 0;
    }
}
