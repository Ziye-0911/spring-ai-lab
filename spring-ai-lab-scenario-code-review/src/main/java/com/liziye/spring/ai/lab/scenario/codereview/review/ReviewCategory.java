package com.liziye.spring.ai.lab.scenario.codereview.review;

/**
 * 代码审查分类枚举。
 *
 * <p>定义了五大审查维度：代码质量、安全性、性能、最佳实践、错误处理。
 *
 * @author liziye
 * @since 1.0.0
 */
public enum ReviewCategory {

    /** 代码质量 */
    CODE_QUALITY("代码质量", "命名规范、代码结构、可读性、注释质量"),

    /** 安全性 */
    SECURITY("安全性", "安全漏洞、输入验证、权限控制、敏感信息泄露"),

    /** 性能 */
    PERFORMANCE("性能", "算法复杂度、资源使用、缓存策略、数据库查询优化"),

    /** 最佳实践 */
    BEST_PRACTICE("最佳实践", "设计模式、SOLID 原则、框架使用规范"),

    /** 错误处理 */
    ERROR_HANDLING("错误处理", "异常捕获、边界条件、空值处理、日志记录");

    private final String label;
    private final String description;

    ReviewCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
