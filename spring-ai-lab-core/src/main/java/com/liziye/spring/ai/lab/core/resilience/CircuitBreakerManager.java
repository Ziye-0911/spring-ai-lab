package com.liziye.spring.ai.lab.core.resilience;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 熔断器管理器 — 封装 Resilience4j CircuitBreaker 操作。
 *
 * <p>支持多个命名熔断器实例，按模型/操作粒度创建。
 * 提供简洁的函数式 API 包裹 LLM 调用。
 *
 * <p>使用条件：仅在类路径包含 Resilience4j 时自动装配。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class CircuitBreakerManager {

    private final CircuitBreakerConfig config;
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreakerManager(CircuitBreakerConfig config) {
        this.config = config;
        log.info("CircuitBreakerManager initialized: failureRateThreshold={}%, waitDurationInOpen={}s",
                config.getFailureRateThreshold(), config.getWaitDurationInOpen().getSeconds());
    }

    /**
     * 获取或创建命名熔断器。
     */
    public CircuitBreaker getOrCreate(String name) {
        return breakers.computeIfAbsent(name, this::createBreaker);
    }

    /**
     * 通过熔断器执行 LLM 调用。
     *
     * @param name    熔断器名称（如 "chat"、"code-review"）
     * @param call    需要保护的 LLM 调用
     * @param fallback 熔断/失败时的降级处理
     * @param <T>    返回类型
     * @return 调用结果
     */
    public <T> T executeWithBreaker(String name,
                                     Supplier<T> call,
                                     java.util.function.Function<Throwable, T> fallback) {
        CircuitBreaker breaker = getOrCreate(name);

        if (!breaker.isOpen()) {
            try {
                T result = call.get();
                breaker.recordSuccess();
                return result;
            } catch (Exception e) {
                breaker.recordFailure(e);
                log.warn("[CIRCUIT-BREAKER] name={} call failed: {}, state={}",
                        name, e.getMessage(), breaker.getState());
                return fallback.apply(e);
            }
        } else {
            log.warn("[CIRCUIT-BREAKER] name={} circuit OPEN, using fallback directly", name);
            return fallback.apply(new CircuitBreakerOpenException(
                    "Circuit breaker [" + name + "] is OPEN"));
        }
    }

    /**
     * 获取熔断器状态。
     */
    public CircuitBreaker.State getState(String name) {
        CircuitBreaker breaker = breakers.get(name);
        return breaker != null ? breaker.getState() : CircuitBreaker.State.CLOSED;
    }

    /**
     * 重置熔断器。
     */
    public void reset(String name) {
        CircuitBreaker breaker = breakers.get(name);
        if (breaker != null) {
            breaker.reset();
        }
    }

    /**
     * 获取所有熔断器的状态。
     */
    public Map<String, CircuitBreaker.State> getAllStates() {
        Map<String, CircuitBreaker.State> states = new ConcurrentHashMap<>();
        for (Map.Entry<String, CircuitBreaker> entry : breakers.entrySet()) {
            states.put(entry.getKey(), entry.getValue().getState());
        }
        return states;
    }

    private CircuitBreaker createBreaker(String name) {
        CircuitBreaker breaker = new CircuitBreaker(
                name,
                config.getFailureRateThreshold(),
                config.getWaitDurationInOpen(),
                config.getSlidingWindowSize(),
                config.getMinimumNumberOfCalls()
        );
        log.info("Created circuit breaker: name={}", name);
        return breaker;
    }

    // ===== 内部熔断器实现 =====

    /**
     * 熔断器配置参数。
     */
    public static class CircuitBreakerConfig {
        private int failureRateThreshold = 50;
        private Duration waitDurationInOpen = Duration.ofSeconds(60);
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;

        // getters/setters / builders
        public int getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(int t) { this.failureRateThreshold = t; }
        public Duration getWaitDurationInOpen() { return waitDurationInOpen; }
        public void setWaitDurationInOpen(Duration d) { this.waitDurationInOpen = d; }
        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int s) { this.slidingWindowSize = s; }
        public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
        public void setMinimumNumberOfCalls(int n) { this.minimumNumberOfCalls = n; }
    }

    /**
     * 基于滑动窗口的熔断器实现。
     *
     * <p>支持三种状态：{@code CLOSED}（正常）、{@code OPEN}（熔断）、{@code HALF_OPEN}（探测恢复）。
     */
    public static class CircuitBreaker {
        private final String name;
        private final int failureRateThreshold;
        private final Duration waitDurationInOpen;
        private final int slidingWindowSize;
        private final int minimumNumberOfCalls;
        private final RingBuffer ringBuffer;
        private volatile State state = State.CLOSED;
        private volatile long openedAt;
        private volatile long lastFailureCount;
        private final Object lock = new Object();

        CircuitBreaker(String name,
                       int failureRateThreshold,
                       Duration waitDurationInOpen,
                       int slidingWindowSize,
                       int minimumNumberOfCalls) {
            this.name = name;
            this.failureRateThreshold = failureRateThreshold;
            this.waitDurationInOpen = waitDurationInOpen;
            this.slidingWindowSize = slidingWindowSize;
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            this.ringBuffer = new RingBuffer(slidingWindowSize);
        }

        public boolean isOpen() {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - openedAt > waitDurationInOpen.toMillis()) {
                    // 转换为 HALF_OPEN，允许探测调用
                    synchronized (lock) {
                        if (state == State.OPEN) {
                            state = State.HALF_OPEN;
                            log.info("[CIRCUIT-BREAKER] name={} state=HALF_OPEN", name);
                        }
                    }
                    return false;
                }
                return true;
            }
            return false;
        }

        public void recordSuccess() {
            synchronized (lock) {
                ringBuffer.recordSuccess();
                if (state == State.HALF_OPEN) {
                    if (ringBuffer.getSuccessRate() >= 0.5) {
                        state = State.CLOSED;
                        ringBuffer.reset();
                        log.info("[CIRCUIT-BREAKER] name={} state=CLOSED (recovered)", name);
                    }
                }
            }
        }

        public void recordFailure(Throwable e) {
            synchronized (lock) {
                ringBuffer.recordFailure();
                lastFailureCount = ringBuffer.getFailureCount();
                log.debug("[CIRCUIT-BREAKER] name={} failureCount={} totalCalls={}",
                        name, lastFailureCount, ringBuffer.getTotalCalls());

                if (shouldOpen()) {
                    state = State.OPEN;
                    openedAt = System.currentTimeMillis();
                    log.warn("[CIRCUIT-BREAKER] name={} state=OPEN failureRate={}/{}",
                            name, ringBuffer.getFailureCount(), ringBuffer.getTotalCalls());
                }
            }
        }

        public void reset() {
            synchronized (lock) {
                state = State.CLOSED;
                ringBuffer.reset();
                log.info("[CIRCUIT-BREAKER] name={} state=CLOSED (reset)", name);
            }
        }

        public State getState() { return state; }

        private boolean shouldOpen() {
            long totalCalls = ringBuffer.getTotalCalls();
            if (totalCalls < minimumNumberOfCalls) {
                return false;
            }
            double failureRate = ringBuffer.getFailureRate();
            return failureRate * 100 >= failureRateThreshold;
        }

        /** 熔断器状态枚举 */
        public enum State { CLOSED, OPEN, HALF_OPEN }

        /**
         * 环形缓冲区，记录最近 N 次调用的成功/失败。
         */
        static class RingBuffer {
            private final int size;
            private final boolean[] buffer;
            private int index;
            private long count;
            private int failureCount;

            RingBuffer(int size) {
                this.size = size;
                this.buffer = new boolean[size];
                this.index = 0;
                this.count = 0;
                this.failureCount = 0;
            }

            synchronized void recordSuccess() {
                if (count >= size) {
                    if (buffer[index]) failureCount--;
                }
                buffer[index] = false;
                index = (index + 1) % size;
                count = Math.min(count + 1, size);
            }

            synchronized void recordFailure() {
                if (count >= size) {
                    if (buffer[index]) failureCount--;
                }
                buffer[index] = true;
                failureCount++;
                index = (index + 1) % size;
                count = Math.min(count + 1, size);
            }

            synchronized void reset() {
                for (int i = 0; i < size; i++) buffer[i] = false;
                index = 0;
                count = 0;
                failureCount = 0;
            }

            long getTotalCalls() { return count; }
            int getFailureCount() { return failureCount; }
            double getFailureRate() { return count == 0 ? 0 : (double) failureCount / count; }
            double getSuccessRate() { return count == 0 ? 1.0 : 1.0 - (double) failureCount / count; }
        }
    }

    /**
     * 熔断器开启异常，当熔断器处于 {@code OPEN} 状态时抛出。
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
