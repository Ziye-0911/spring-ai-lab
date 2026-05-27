package com.liziye.spring.ai.lab.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对话记忆配置属性。
 *
 * <p>配置前缀: {@code spring.ai.lab.memory}
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.memory")
public class MemoryProperties {

    /** 记忆类型: in-memory / redis */
    private String type = "in-memory";

    /** 最大保留历史消息数 */
    private int maxHistory = 20;

    /** 会话过期时间（分钟） */
    private int ttlMinutes = 30;

    /** 定时清理间隔（分钟） */
    private int cleanupIntervalMinutes = 60;

    /** Redis 配置（type=redis 时生效） */
    private Redis redis = new Redis();

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String keyPrefix = "ailab:memory:";
        private int ttlMinutes = 30;
    }
}
