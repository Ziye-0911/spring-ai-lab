package com.liziye.spring.ai.lab.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DashScope API 配置属性。
 *
 * <p>支持阿里云 DashScope 的 OpenAI 兼容端点。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.dashscope")
public class DashScopeProperties {

    /** API Key */
    private String apiKey = "";

    /** API 基础地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com/apps/anthropic";

    /** 使用的模型名称 */
    private String model = "claude-3-sonnet-20240229";

    /** 默认最大 Token 数 */
    private int maxTokens = 2048;

    /** 默认温度 */
    private double temperature = 0.7;

    /** 连接超时（秒） */
    private int connectTimeout = 30;

    /** 读取超时（秒） */
    private int readTimeout = 120;
}
