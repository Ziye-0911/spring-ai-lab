package com.liziye.spring.ai.lab.core.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 延迟统计指标 — 按操作类型统计延迟（毫秒）。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class LatencyMetrics {

    private final Map<String, AtomicLong> totalLatencyByOperation = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> countByOperation = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> maxLatencyByOperation = new ConcurrentHashMap<>();

    /**
     * 记录某操作的延迟。
     *
     * @param operation 操作名称
     * @param latencyMs 延迟时长（毫秒）
     */
    public void recordLatency(String operation, long latencyMs) {
        totalLatencyByOperation.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(latencyMs);
        countByOperation.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
        maxLatencyByOperation.computeIfAbsent(operation, k -> new AtomicLong(0))
                .updateAndGet(current -> Math.max(current, latencyMs));
        log.debug("[METRICS-LATENCY] operation={} latency={}ms", operation, latencyMs);
    }

    /**
     * 获取指定操作的平均延迟。
     *
     * @param operation 操作名称
     * @return 平均延迟（毫秒）
     */
    public double getAverageLatency(String operation) {
        AtomicLong total = totalLatencyByOperation.get(operation);
        AtomicLong count = countByOperation.get(operation);
        if (total == null || count == null || count.get() == 0) {
            return 0;
        }
        return (double) total.get() / count.get();
    }

    /**
     * 获取指定操作的最大延迟。
     *
     * @param operation 操作名称
     * @return 最大延迟（毫秒）
     */
    public long getMaxLatency(String operation) {
        AtomicLong max = maxLatencyByOperation.get(operation);
        return max != null ? max.get() : 0;
    }

    /**
     * 获取延迟汇总信息。
     *
     * @return 包含平均延迟和最大延迟的 Map
     */
    public Map<String, Object> getSummary() {
        Map<String, Double> avgLatency = new ConcurrentHashMap<>();
        countByOperation.forEach((op, count) ->
                avgLatency.put(op, getAverageLatency(op)));
        return Map.of(
                "averageLatency", avgLatency,
                "maxLatency", Map.copyOf(maxLatencyByOperation)
        );
    }
}
