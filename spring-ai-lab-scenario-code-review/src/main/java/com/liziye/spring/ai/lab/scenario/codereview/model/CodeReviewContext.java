package com.liziye.spring.ai.lab.scenario.codereview.model;

import com.liziye.spring.ai.lab.core.model.AgentContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码审查专属上下文。
 *
 * <p>继承自 {@link AgentContext}，扩展了 Diff 内容、审查来源、
 * 文件列表、代码语言等审查相关字段。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeReviewContext extends AgentContext {

    /** 审查的代码变更（Diff 文本） */
    private String diffContent;

    /** 审查来源：git-diff / manual */
    private String diffSource;

    /** 审查的文件列表 */
    private List<String> changedFiles = new ArrayList<>();

    /** 代码语言（如 java、python、javascript） */
    private String language;

    /** 是否只审查关键问题（跳过风格建议） */
    private boolean criticalOnly = false;
}
