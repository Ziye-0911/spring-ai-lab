package com.liziye.spring.ai.lab.core.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全与限流配置属性。
 *
 * <p>配置前缀：{@code spring.ai.lab.security}
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.security")
public class SecurityProperties {

    /** 是否启用内建安全机制 */
    private boolean enabled = false;

    /** 速率限制配置 */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 速率限制内部配置类。
     */
    @Data
    public static class RateLimit {

        /** 是否启用限流 */
        private boolean enabled = false;

        /** 对话场景：每秒允许请求数 */
        private double chatPermitsPerSecond = 10;

        /** RAG 场景：每秒允许请求数 */
        private double ragPermitsPerSecond = 5;

        /** 数据分析场景：每秒允许请求数 */
        private double dataAnalysisPermitsPerSecond = 3;

        /** 文档上传：每秒允许请求数 */
        private double documentUploadPermitsPerSecond = 2;

        /** 整体每秒允许请求数 */
        private double globalPermitsPerSecond = 20;

        /** 限流超出时返回的 HTTP 状态码 */
        private int tooManyRequestsStatus = 429;

        /** 限流超出时的提示信息 */
        private String tooManyRequestsMessage = "请求过于频繁，请稍后重试";
    }
}
