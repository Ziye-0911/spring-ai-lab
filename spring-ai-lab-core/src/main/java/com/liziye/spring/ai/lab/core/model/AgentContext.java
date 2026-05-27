package com.liziye.spring.ai.lab.core.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 上下文基类 — 所有场景 Context 的父类。
 *
 * <p>包含所有场景共用的上下文信息。
 * 子类通过继承添加场景专属字段。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
public class AgentContext {

    /** 会话ID */
    protected String conversationId;

    /** 使用的模型提供商名称（如 "openai"、"ollama"） */
    protected String modelProvider;

    /** 最大 Token 数 */
    protected Integer maxTokens;

    /** 温度参数 */
    protected Double temperature;

    /** 附加元数据（仅用于框架内部传递，业务不应依赖） */
    protected Map<String, Object> metadata = new HashMap<>();
}
