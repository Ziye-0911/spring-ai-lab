package com.liziye.spring.ai.lab.scenario.dataanalysis;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用数据分析 Agent 场景。
 *
 * <p>在 Spring Boot 应用启动类添加此注解，自动装配 NL2SQL 数据分析组件：
 * 自然语言转 SQL、Schema 自动提取、SQL 执行、AI 分析结果。
 *
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableDataAnalysis
 * public class MyApp {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApp.class, args);
 *     }
 * }
 * </pre>
 *
 * <p>支持配置数据源类型和是否自动提取 Schema。</p>
 *
 * @author liziye
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DataAnalysisAutoConfiguration.class)
public @interface EnableDataAnalysis {

    /** 数据源类型（mysql / postgresql / h2） */
    String datasourceType() default "mysql";

    /** 是否自动提取数据库 Schema */
    boolean autoExtractSchema() default true;
}
