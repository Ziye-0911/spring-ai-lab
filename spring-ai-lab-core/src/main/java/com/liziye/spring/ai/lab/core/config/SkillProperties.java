package com.liziye.spring.ai.lab.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Skill 系统配置属性。
 *
 * <p>配置前缀: {@code spring.ai.lab.skill}
 *
 * @author liziye
 * @since 0.3.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.skill")
public class SkillProperties {

    /** 是否启用 Skill 系统 */
    private boolean enabled = true;

    /** Skill 文件目录（相对路径、绝对路径或 classpath: 前缀） */
    private String directory = "skills";

    /** 是否启用热加载（监听文件变更自动更新） */
    private boolean hotReload = true;

    /** 路由策略：semantic（语义匹配）、keyword（关键词）、llm（LLM 路由） */
    private String routingStrategy = "semantic";

    /** 语义匹配的相似度阈值（0-1，低于此值不匹配） */
    private double similarityThreshold = 0.1;

    /** 每次请求最多匹配的 Skill 数量 */
    private int maxMatchedSkills = 3;
}
