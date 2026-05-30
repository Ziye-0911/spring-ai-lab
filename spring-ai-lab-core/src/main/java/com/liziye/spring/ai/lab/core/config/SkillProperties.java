package com.liziye.spring.ai.lab.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Skill 系统配置属性。
 *
 * <p>配置前缀: {@code spring.ai.lab.skill}
 *
 * <p><b>多源加载策略（优先级从高到低）：</b>
 * <ol>
 *   <li>REST API 动态注册（运行时，内存）</li>
 *   <li>外部文件目录 {@link #externalDir}（文件系统，可读写，支持热加载）</li>
 *   <li>classpath:skills（JAR 内置，只读，作为兜底默认）</li>
 * </ol>
 *
 * <p>同名 Skill 高优先级覆盖低优先级。
 *
 * @author liziye
 * @since 0.3.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.skill")
public class SkillProperties {

    /** 是否启用 Skill 系统 */
    private boolean enabled = true;

    /** Skill 内置文件目录（classpath 路径，仅当启用外部目录时作为兜底来源） */
    private String directory = "skills";

    /**
     * 外部 Skill 文件目录（文件系统路径，可读写）。
     *
     * <p><b>用户生产环境核心配置：</b>设置为可写的文件系统目录后即可动态管理 Skill。
     * 该目录下的 Skill 优先于 classpath 内置 Skill，同名 Skill 以该目录为准。
     *
     * <p>示例: {@code /opt/myapp/skills} 或 {@code ./custom-skills}
     *
     * <p>若未配置（默认 null），则仅从 {@link #directory} 加载。
     */
    private String externalDir = null;

    /**
     * 是否在首次启动时将 classpath 内置 Skill 复制到外部目录。
     *
     * <p>仅在 {@link #externalDir} 已配置、且外部目录为空或不存在时生效。
     * <p>适用于生产环境首次部署：自动种子化内置 Skill 到外部目录，之后用户可自由编辑。
     */
    private boolean autoInit = true;

    /**
     * 是否启用 Skill 管理 REST API。
     *
     * <p>启用后暴露 {@code /api/skills/} 端点，支持 CRUD 管理外部目录的 Skill 文件。
     * <p>需要 classpath 中存在 spring-boot-starter-web。
     */
    private boolean enableManagement = false;

    /** 是否启用热加载（监听外部文件目录变更，自动更新） */
    private boolean hotReload = true;

    /** 路由策略：semantic（语义匹配）、keyword（关键词）、llm（LLM 路由） */
    private String routingStrategy = "semantic";

    /** 语义匹配的相似度阈值（0-1，低于此值不匹配） */
    private double similarityThreshold = 0.1;

    /** 每次请求最多匹配的 Skill 数量 */
    private int maxMatchedSkills = 3;
}
