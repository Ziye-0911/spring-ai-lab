package com.liziye.spring.ai.lab.core.skill;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 已解析的 Skill — 从 .md 文件中提取的结构化数据。
 *
 * <p>Skill 文件采用 YAML Frontmatter + Markdown 正文格式：
 * <pre>{@code
 * ---
 * name: weather-assistant
 * displayName: 天气助手
 * description: 提供天气查询能力
 * category: utility
 * tags: [天气, 气象]
 * model:
 *   temperature: 0.3
 *   maxTokens: 1024
 * ---
 * # 角色
 * 你是一个专业的天气查询助手...
 * }</pre>
 *
 * @author liziye
 * @since 0.3.0
 */
@Data
@Builder
public class ParsedSkill {

    /** Skill 唯一标识（文件名去扩展名） */
    private String name;

    /** 展示名称 */
    private String displayName;

    /** 技能描述（用于路由匹配，用户意图与此最相近的 Skill 将被选中） */
    private String description;

    /** 分类 */
    private String category;

    /** 标签 */
    private List<String> tags;

    /** 优先级（数值越大越优先，默认 0） */
    private int priority;

    /** 模型参数 */
    private ModelConfig model;

    /** Skill 主体内容 — 系统提词正文 */
    private String body;

    /** 源文件路径 */
    private Path sourcePath;

    /** 最后修改时间 */
    private long lastModified;

    /** 原始 YAML 元数据（保留所有 frontmatter 字段，便于扩展） */
    private Map<String, Object> rawMetadata;

    /**
     * 模型参数配置。
     */
    @Data
    @Builder
    public static class ModelConfig {
        /** 温度 */
        private Double temperature;
        /** 最大 Token 数 */
        private Integer maxTokens;
        /** Top-P 采样 */
        private Double topP;
    }
}
