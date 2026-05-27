package com.liziye.spring.ai.lab.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring AI Lab 全局配置属性。
 *
 * <p>配置前缀: {@code spring.ai.lab}
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab")
public class LabProperties {

    /** 是否启用 Spring AI Lab */
    private boolean enabled = true;

    /** 可观测性配置 */
    private Observation observation = new Observation();

    /** 记忆配置 */
    private Memory memory = new Memory();

    /** 模型配置 */
    private Model model = new Model();

    @Data
    public static class Observation {
        private boolean enabled = true;
        private boolean tokenTracking = true;
        private boolean latencyTracking = true;
        private boolean metricsExport = true;
        private String exportPrefix = "ai_lab";
    }

    @Data
    public static class Memory {
        private String type = "in-memory";
        private int maxHistory = 20;
        private int ttlMinutes = 30;
        private int cleanupIntervalMinutes = 60;
    }

    @Data
    public static class Model {
        private String primaryProvider = "openai";
        private String fallbackProvider;
    }
}
