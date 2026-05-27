package com.liziye.spring.ai.lab.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent 响应统一模型。
 *
 * <p>封装一次 Agent 调用的所有响应信息，
 * 包括 AI 回复内容、工具调用记录和元数据。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /** AI 回复内容 */
    private String content;

    /** 会话ID */
    private String conversationId;

    /** 工具调用记录 */
    private List<ToolCall> toolCalls;

    /** 元数据（Token数、延迟、模型名等） */
    private Map<String, Object> metadata;

    /** 是否来自降级响应 */
    @Builder.Default
    private boolean fallback = false;
}
