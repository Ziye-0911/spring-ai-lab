package com.liziye.spring.ai.lab.core.advisor;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 重试增强器 — 支持指数退避重试策略。
 *
 * <p>当 AI 模型调用因网络超时等可重试异常失败时自动重试。
 *
 * <p>配置示例：
 * <pre>
 * spring.ai.lab.retry.max-attempts=3
 * spring.ai.lab.retry.backoff-strategy=exponential
 * spring.ai.lab.retry.initial-delay-ms=1000
 * spring.ai.lab.retry.max-delay-ms=10000
 * spring.ai.lab.retry.multiplier=2.0
 * </pre>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class RetryAdvisor {

    /** 最大重试次数 */
    private final int maxAttempts;

    /** 退避策略 */
    private final BackoffStrategy backoffStrategy;

    /** 初始延迟（毫秒） */
    private final long initialDelayMs;

    /** 最大延迟（毫秒） */
    private final long maxDelayMs;

    /** 指数退避倍数 */
    private final double multiplier;

    /** 可重试的异常类型集合 */
    private final Set<Class<? extends Throwable>> retryableExceptions;

    /**
     * 构造重试增强器。
     *
     * @param maxAttempts    最大重试次数
     * @param backoffStrategy 退避策略名称（{@code fixed} 或 {@code exponential}）
     * @param initialDelayMs 初始延迟（毫秒）
     * @param maxDelayMs     最大延迟（毫秒）
     * @param multiplier     指数退避倍数
     */
    public RetryAdvisor(int maxAttempts, String backoffStrategy,
                        long initialDelayMs, long maxDelayMs, double multiplier) {
        this.maxAttempts = maxAttempts;
        this.backoffStrategy = BackoffStrategy.from(backoffStrategy);
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
        this.retryableExceptions = Set.of(
                java.net.SocketTimeoutException.class,
                java.io.IOException.class
        );
    }

    /**
     * 判断当前尝试是否应该重试。
     *
     * @param attempt   当前尝试次数（从 0 开始）
     * @param exception 发生的异常
     * @return {@code true} 应重试，{@code false} 不应重试
     */
    public boolean shouldRetry(int attempt, Throwable exception) {
        if (attempt >= maxAttempts) {
            return false;
        }
        return retryableExceptions.stream().anyMatch(e -> e.isAssignableFrom(exception.getClass()));
    }

    /**
     * 根据退避策略计算下一次重试的等待时间。
     *
     * @param attempt 当前尝试次数
     * @return 等待时间（毫秒）
     */
    public long computeDelay(int attempt) {
        long delay;
        if (backoffStrategy == BackoffStrategy.EXPONENTIAL) {
            delay = initialDelayMs * (long) Math.pow(multiplier, attempt);
        } else {
            delay = initialDelayMs;
        }
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 执行重试前的等待。
     *
     * @param attempt 当前尝试次数
     * @throws RuntimeException 等待被中断时抛出
     */
    public void waitBeforeRetry(int attempt) {
        long delay = computeDelay(attempt);
        log.info("[RETRY] Attempt {}/{}, waiting {}ms before retry", attempt, maxAttempts, delay);
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", e);
        }
    }

    /**
     * 获取最大重试次数。
     *
     * @return 最大重试次数
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 退避策略枚举。
     */
    public enum BackoffStrategy {
        /** 固定延迟 */
        FIXED,
        /** 指数退避 */
        EXPONENTIAL;

        /**
         * 根据策略名称解析退避策略。
         *
         * @param name 策略名称
         * @return 对应的 {@link BackoffStrategy}，未知名称默认为 {@link #FIXED}
         */
        public static BackoffStrategy from(String name) {
            if ("exponential".equalsIgnoreCase(name)) {
                return EXPONENTIAL;
            }
            return FIXED;
        }
    }
}
