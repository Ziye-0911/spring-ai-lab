package com.liziye.spring.ai.lab.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 请求统一模型。
 *
 * <p>封装一次 Agent 调用的所有请求参数。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

    /** 会话ID */
    private String conversationId;

    /** 用户输入 */
    private String userInput;

    /** 使用的模型名称 */
    private String modelName;

    /** 最大 Token 数 */
    private Integer maxTokens;

    /** 温度参数 */
    private Double temperature;

    /** 扩展参数 */
    private Map<String, Object> parameters;
}
