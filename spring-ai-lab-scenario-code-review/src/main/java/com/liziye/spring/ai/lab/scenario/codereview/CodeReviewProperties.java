package com.liziye.spring.ai.lab.scenario.codereview;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Code Review 场景配置属性。
 *
 * <p>绑定 {@code spring.ai.lab.code-review} 前缀的配置项，支持自定义系统 Prompt、
 * 审查参数和 Diff 大小限制。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.code-review")
public class CodeReviewProperties {

    /** 默认系统提示 */
    private String systemPrompt = """
            你是一个资深的代码审查专家。请对以下代码变更进行专业审查。
                        
            审查维度：
            1. 代码质量 - 命名规范、代码结构、可读性
            2. 安全性 - 潜在安全漏洞、输入验证
            3. 性能 - 性能瓶颈、资源浪费
            4. 最佳实践 - 是否遵循业界最佳实践
            5. 错误处理 - 异常场景是否覆盖完整
                        
            输出格式：
            - 问题严重程度: [严重/一般/建议]
            - 文件/行号
            - 问题描述
            - 修改建议
            """;

    /** 默认温度 */
    private double temperature = 0.5;

    /** 默认最大 Token */
    private int maxTokens = 4096;

    /** 最大 Diff 大小（行数） */
    private int maxDiffLines = 1000;
}
