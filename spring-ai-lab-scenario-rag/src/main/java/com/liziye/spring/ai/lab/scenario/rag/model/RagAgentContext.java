package com.liziye.spring.ai.lab.scenario.rag.model;

import com.liziye.spring.ai.lab.core.model.AgentContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 场景专属上下文。
 *
 * <p>扩展基类 {@link com.liziye.spring.ai.lab.core.model.AgentContext}，
 * 增加检索相关参数：Top-K、相似度阈值、重排序开关、知识库名称。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RagAgentContext extends AgentContext {

    /** 检索 Top-K */
    private int topK = 5;

    /** 相似度阈值 */
    private double similarityThreshold = 0.7;

    /** 是否启用重排序 */
    private boolean rerankEnabled = false;

    /** 知识库名称 */
    private String knowledgeBase;
}
