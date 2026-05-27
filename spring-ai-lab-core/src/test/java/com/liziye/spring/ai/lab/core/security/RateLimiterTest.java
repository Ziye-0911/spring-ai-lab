package com.liziye.spring.ai.lab.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 令牌桶限流器单元测试。
 */
class RateLimiterTest {

    @Test
    @DisplayName("应允许速率为 N 的请求")
    void shouldAllowRequestsWithinLimit() {
        RateLimiter limiter = new RateLimiter(10.0);
        String key = "test-key";

        // 前10个请求应该被允许
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(key))
                    .as("request #%d should be allowed", i + 1)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("超出速率应被拒绝")
    void shouldRejectExceedingRequests() {
        RateLimiter limiter = new RateLimiter(5.0);
        String key = "test-key";

        // 消耗所有令牌
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire(key);
        }

        // 第6个请求应被拒绝
        assertThat(limiter.tryAcquire(key)).isFalse();
    }

    @Test
    @DisplayName("不同 Key 应独立限流")
    void shouldLimitKeysIndependently() {
        RateLimiter limiter = new RateLimiter(2.0);

        // key1 用完令牌
        limiter.tryAcquire("key1");
        limiter.tryAcquire("key1");

        // key2 应该仍然可以使用
        assertThat(limiter.tryAcquire("key2")).isTrue();
        assertThat(limiter.tryAcquire("key2")).isTrue();

        // key2 也用完了
        assertThat(limiter.tryAcquire("key2")).isFalse();
    }

    @Test
    @DisplayName("reset 后可以重新获取")
    void shouldAllowRequestsAfterReset() {
        RateLimiter limiter = new RateLimiter(3.0);
        String key = "test";

        // 用完
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire(key);
        }
        assertThat(limiter.tryAcquire(key)).isFalse();

        // 重置
        limiter.reset(key);

        // 可以重新获取
        assertThat(limiter.tryAcquire(key)).isTrue();
    }

    @Test
    @DisplayName("应追踪活跃 Key 数")
    void shouldTrackActiveKeys() {
        RateLimiter limiter = new RateLimiter(10.0);

        limiter.tryAcquire("a");
        limiter.tryAcquire("b");
        limiter.tryAcquire("c");

        assertThat(limiter.getActiveKeys()).isEqualTo(3);

        limiter.reset("a");
        assertThat(limiter.getActiveKeys()).isEqualTo(2);
    }

    @Test
    @DisplayName("SecurityProperties 默认值")
    void shouldHaveSensibleDefaults() {
        SecurityProperties props = new SecurityProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getRateLimit().isEnabled()).isFalse();
        assertThat(props.getRateLimit().getChatPermitsPerSecond()).isEqualTo(10);
        assertThat(props.getRateLimit().getRagPermitsPerSecond()).isEqualTo(5);
        assertThat(props.getRateLimit().getTooManyRequestsStatus()).isEqualTo(429);
    }
}
