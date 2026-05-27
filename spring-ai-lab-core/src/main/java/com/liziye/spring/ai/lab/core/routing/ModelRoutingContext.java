package com.liziye.spring.ai.lab.core.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 模型路由上下文 — 包含路由决策所需的所有信息。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRoutingContext {

    /** 请求头中指定的模型名称（如 X-AI-Model: ollama） */
    private String requestHeaderModel;

    /** 配置中指定的默认模型 */
    private String configuredDefaultModel;

    /** 用户请求内容（可用于分析复杂度） */
    private String userInput;

    /** 会话ID */
    private String conversationId;

    /** 扩展属性 */
    private Map<String, Object> attributes;
}
