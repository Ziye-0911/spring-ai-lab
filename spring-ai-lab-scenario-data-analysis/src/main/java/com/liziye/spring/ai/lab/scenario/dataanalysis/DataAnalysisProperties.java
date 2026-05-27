package com.liziye.spring.ai.lab.scenario.dataanalysis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据分析场景配置属性。
 *
 * <p>配置前缀：{@code spring.ai.lab.data-analysis}。
 * 包含数据源配置（JDBC URL、用户名、密码等）、NL2SQL 系统提示、
 * 数据分析系统提示以及查询参数（最大行数、超时时间等）。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.data-analysis")
public class DataAnalysisProperties {

    /** 数据源类型 */
    private String datasourceType = "mysql";

    /** 是否自动提取 Schema */
    private boolean autoExtractSchema = true;

    /** 数据库连接 URL */
    private String jdbcUrl = "jdbc:mysql://localhost:3306/ai_lab?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";

    /** 数据库用户名 */
    private String username = "root";

    /** 数据库密码 */
    private String password = "root";

    /** 默认温度 */
    private double temperature = 0.3;

    /** 默认最大 Token */
    private int maxTokens = 4096;

    /** 最大返回行数 */
    private int maxRows = 100;

    /** 查询超时时间（秒） */
    private int queryTimeoutSeconds = 30;

    /** NL2SQL 系统提示 */
    private String nl2sqlSystemPrompt = """
            你是一个精确的 SQL 生成器。请根据用户的自然语言描述和提供的数据库 Schema，
            生成可执行的 SQL 查询语句。
                        
            规则：
            1. 只生成 SELECT 语句（只读查询）
            2. 使用标准 SQL 语法
            3. 对表名和列名使用反引号或双引号
            4. 对条件中的字符串值使用单引号
            5. 添加必要的 WHERE 条件防止全表扫描
            6. 为聚合查询添加 GROUP BY
            7. 只输出 SQL 语句，不要解释
            """;

    /** 分析系统提示 */
    private String analysisSystemPrompt = """
            你是一个数据分析专家。请对 SQL 查询结果进行专业的数据分析。
                        
            分析维度：
            1. 数据概况 - 总体趋势、关键指标
            2. 异常发现 - 值得关注的数据点
            3. 业务洞察 - 数据背后的业务含义
            4. 建议 - 基于分析结果的操作建议
                        
            请用中文给出简洁专业的分析。
            """;
}
