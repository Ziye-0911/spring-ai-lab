package com.liziye.spring.ai.lab.core.security;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简易令牌桶限流器。
 *
 * <p>不依赖外部框架，纯内存实现。支持按 Key（如接口路径）独立限流。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * RateLimiter limiter = new RateLimiter(10.0); // 每秒10个令牌
 * if (limiter.tryAcquire("key")) {
 *     // 业务逻辑
 * } else {
 *     // 触发限流
 * }
 * </pre>
 */
@Slf4j
public class RateLimiter {

    /** 每秒生成令牌数 */
    private final double permitsPerSecond;

    /** Key → 令牌桶 */
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * 构造限流器。
     *
     * @param permitsPerSecond 每秒生成令牌数
     */
    public RateLimiter(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    /**
     * 尝试获取一个令牌。
     *
     * @param key 限流 Key
     * @return true 获取成功，false 被限流
     */
    public boolean tryAcquire(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(permitsPerSecond));
        return bucket.tryAcquire();
    }

    /**
     * 获取指定 Key 的当前可用令牌数（用于监控）。
     *
     * @param key 限流 Key
     * @return 当前可用令牌数
     */
    public double getAvailableTokens(String key) {
        TokenBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getAvailableTokens() : permitsPerSecond;
    }

    /**
     * 获取活跃的限流 Key 数量。
     */
    public int getActiveKeys() {
        return buckets.size();
    }

    /**
     * 重置指定 Key。
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * 清除所有桶。
     */
    public void clearAll() {
        buckets.clear();
    }

    // ===== 令牌桶实现 =====

    static class TokenBucket {
        private final double permitsPerSecond;
        private final double maxTokens;
        private volatile double availableTokens;
        private volatile long lastRefillTime;

        TokenBucket(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
            this.maxTokens = permitsPerSecond; // 最大积攒 1 秒
            this.availableTokens = permitsPerSecond;
            this.lastRefillTime = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            refill();
            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized double getAvailableTokens() {
            refill();
            return availableTokens;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;
            double tokensToAdd = elapsedSeconds * permitsPerSecond;
            availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
            lastRefillTime = now;
        }
    }
}
