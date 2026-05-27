package com.liziye.spring.ai.lab.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 多模型提供商配置。
 *
 * <p>配置前缀: {@code spring.ai.lab.model-group}
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.model-group")
public class ModelProviderProperties {

    /** 默认模型组 */
    private String defaultGroup = "openai";

    /** 降级模型组 */
    private String fallback = "ollama";

    /** 低价模型组 */
    private String cheap;

    /** 模型组映射 (组名 -> 模型名) */
    private Map<String, String> groups = new HashMap<>();
}
