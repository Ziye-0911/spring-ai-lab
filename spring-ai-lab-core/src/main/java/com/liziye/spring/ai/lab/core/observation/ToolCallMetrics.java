package com.liziye.spring.ai.lab.core.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具调用指标 — 按工具名称统计调用成功/失败/耗时。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class ToolCallMetrics {

    private final Map<String, AtomicLong> successCallsByTool = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failedCallsByTool = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalTimeByTool = new ConcurrentHashMap<>();

    /**
     * 记录一次工具调用。
     *
     * @param toolName 工具名称
     * @param success  是否成功
     * @param timeMs   调用耗时（毫秒）
     */
    public void recordCall(String toolName, boolean success, long timeMs) {
        if (success) {
            successCallsByTool.computeIfAbsent(toolName, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            failedCallsByTool.computeIfAbsent(toolName, k -> new AtomicLong(0)).incrementAndGet();
        }
        totalTimeByTool.computeIfAbsent(toolName, k -> new AtomicLong(0)).addAndGet(timeMs);
        log.debug("[METRICS-TOOL] tool={} success={} time={}ms", toolName, success, timeMs);
    }

    /**
     * 获取指定工具的成功率。
     *
     * @param toolName 工具名称
     * @return 成功率（0.0 ~ 1.0）
     */
    public double getSuccessRate(String toolName) {
        long success = getSuccessCount(toolName);
        long failed = getFailedCount(toolName);
        long total = success + failed;
        return total > 0 ? (double) success / total : 0;
    }

    /**
     * 获取指定工具的成功调用次数。
     *
     * @param toolName 工具名称
     * @return 成功次数
     */
    public long getSuccessCount(String toolName) {
        AtomicLong count = successCallsByTool.get(toolName);
        return count != null ? count.get() : 0;
    }

    /**
     * 获取指定工具失败调用次数。
     *
     * @param toolName 工具名称
     * @return 失败次数
     */
    public long getFailedCount(String toolName) {
        AtomicLong count = failedCallsByTool.get(toolName);
        return count != null ? count.get() : 0;
    }

    /**
     * 获取指定工具的平均调用耗时。
     *
     * @param toolName 工具名称
     * @return 平均耗时（毫秒）
     */
    public double getAverageTime(String toolName) {
        AtomicLong totalTime = totalTimeByTool.get(toolName);
        long success = getSuccessCount(toolName);
        long failed = getFailedCount(toolName);
        long total = success + failed;
        if (totalTime == null || total == 0) {
            return 0;
        }
        return (double) totalTime.get() / total;
    }
}
