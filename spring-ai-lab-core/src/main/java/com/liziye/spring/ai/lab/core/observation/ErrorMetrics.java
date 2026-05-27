package com.liziye.spring.ai.lab.core.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 错误率指标 — 按错误类型统计错误发生次数。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class ErrorMetrics {

    private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    private final AtomicLong totalErrors = new AtomicLong(0);

    /**
     * 记录指定类型的错误一次。
     *
     * @param errorType 错误类型名称
     */
    public void recordError(String errorType) {
        errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        totalErrors.incrementAndGet();
        log.warn("[METRICS-ERROR] type={}", errorType);
    }

    /**
     * 通过异常对象记录错误。
     *
     * @param e 异常对象，取其类名作为错误类型
     */
    public void recordError(Throwable e) {
        String errorType = e.getClass().getSimpleName();
        recordError(errorType);
    }

    /**
     * 获取指定类型的错误计数。
     *
     * @param errorType 错误类型名称
     * @return 该类型的错误次数
     */
    public long getErrorCount(String errorType) {
        AtomicLong count = errorsByType.get(errorType);
        return count != null ? count.get() : 0;
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }

    /**
     * 获取所有错误汇总。
     *
     * @return 包含总错误数和按类型分布的 Map
     */
    public Map<String, Object> getSummary() {
        return Map.of(
                "totalErrors", totalErrors.get(),
                "byType", Map.copyOf(errorsByType)
        );
    }
}
