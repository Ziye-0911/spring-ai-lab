package com.liziye.spring.ai.lab.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用记录 — 记录 Agent 调用工具的过程。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    /** 工具名称 */
    private String toolName;

    /** 调用参数 (JSON 格式) */
    private String arguments;

    /** 调用结果 (JSON 格式) */
    private String result;

    /** 是否调用成功 */
    private boolean success;

    /** 错误信息（失败时） */
    private String errorMessage;
}
