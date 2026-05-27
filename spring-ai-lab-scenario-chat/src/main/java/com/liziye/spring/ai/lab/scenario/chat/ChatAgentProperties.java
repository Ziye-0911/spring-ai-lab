package com.liziye.spring.ai.lab.scenario.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chat Agent 场景配置属性。
 *
 * <p>绑定 {@code spring.ai.lab.chat} 前缀的配置项，支持自定义系统 Prompt、
 * 流式输出、对话记忆等参数。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.chat")
public class ChatAgentProperties {

    /** 默认系统 Prompt */
    private String systemPrompt = "你是一个乐于助人的 AI 助手，请用简洁清晰的语言回答用户的问题。";

    /** 是否启用流式输出 */
    private boolean streamingEnabled = true;

    /** 是否启用对话记忆 */
    private boolean memoryEnabled = true;

    /** 默认温度，控制生成文本的随机性 */
    private double temperature = 0.7;

    /** 默认最大 Token 数 */
    private int maxTokens = 2048;

    /** 默认模型名称 */
    private String defaultModel = "dashscope";
}
