package com.liziye.spring.ai.lab.scenario.dataanalysis.orchestrator;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import com.liziye.spring.ai.lab.scenario.dataanalysis.DataAnalysisProperties;
import com.liziye.spring.ai.lab.scenario.dataanalysis.model.DataAnalysisContext;
import com.liziye.spring.ai.lab.scenario.dataanalysis.nl2sql.SchemaExtractor;
import com.liziye.spring.ai.lab.scenario.dataanalysis.nl2sql.SqlGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * 数据分析 Agent 编排器（NL2SQL）。
 *
 * <p>继承 {@link com.liziye.spring.ai.lab.core.orchestrator.BaseOrchestrator}，
 * 实现完整的 NL2SQL 数据分析流程：
 * <ol>
 *   <li>获取/提取数据库 Schema</li>
 *   <li>自然语言 → SQL 生成</li>
 *   <li>执行 SQL 查询（可选）</li>
 *   <li>AI 分析查询结果</li>
 * </ol>
 *
 * <p>使用 {@link SchemaExtractor} 提取表结构，{@link SqlGenerator} 生成和执行 SQL。
 * 支持预设 Mock Schema 用于无数据库连接时的演示。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class DataAnalysisAgent extends BaseOrchestrator<DataAnalysisContext> {

    private final DataAnalysisProperties properties;
    private final SchemaExtractor schemaExtractor;
    private final SqlGenerator sqlGenerator;

    // 预设 Mock Schema（用于无数据库连接时）
    private Map<String, DataAnalysisContext.TableSchema> presetSchema;

    public DataAnalysisAgent(ModelProviderManager modelManager,
                              ConversationMemory memory,
                              List<Advisor> advisors,
                              TokenMetrics tokenMetrics,
                              LatencyMetrics latencyMetrics,
                              DataAnalysisProperties properties) {
        super(modelManager, memory, advisors, tokenMetrics, latencyMetrics);
        this.properties = properties;
        this.schemaExtractor = new SchemaExtractor();
        this.sqlGenerator = new SqlGenerator(properties);
        this.presetSchema = buildMockSchema();
    }

    @Override
    protected AgentResponse doExecute(ChatClient chatClient, String userInput, DataAnalysisContext context) {
        long startTime = System.currentTimeMillis();

        // 1. 获取 Schema
        Map<String, DataAnalysisContext.TableSchema> schemas = context.getTableSchemas();
        if (schemas == null || schemas.isEmpty()) {
            schemas = presetSchema;
        }
        String schemaText = schemaExtractor.formatForPrompt(schemas);

        // 2. NL → SQL
        String sql = sqlGenerator.generateSql(chatClient, userInput, schemaText);
        context.setGeneratedSql(sql);
        log.info("[DATA-ANALYSIS] Generated SQL: {}", sql);

        // 3. 执行 SQL（可选）
        String queryResultText = null;
        if (!context.isSqlOnly() && isSelectQuery(sql)) {
            try {
                List<Map<String, Object>> results = executeSqlInternal(sql, context);
                context.setQueryResult(results);
                queryResultText = sqlGenerator.formatResults(results);
            } catch (Exception e) {
                log.error("[DATA-ANALYSIS] SQL execution failed: {}", e.getMessage());
                queryResultText = "（SQL 执行失败: " + e.getMessage() + "）";
            }
        }

        // 4. AI 分析结果
        StringBuilder content = new StringBuilder();
        content.append("## 自然语言查询\n").append(userInput).append("\n\n");
        content.append("## 生成的 SQL\n```sql\n").append(sql).append("\n```\n\n");

        if (queryResultText != null) {
            content.append("## 查询结果\n").append(queryResultText).append("\n\n");

            // 如果有结果，AI 分析
            if (context.getQueryResult() != null && !context.getQueryResult().isEmpty()) {
                String analysis = sqlGenerator.analyzeResults(chatClient, userInput, sql, queryResultText);
                content.append("## 数据分析\n").append(analysis);
            }
        } else if (context.isSqlOnly()) {
            content.append("## 说明\n仅生成 SQL，未执行查询。使用 sqlOnly=false 可执行查询。\n");
        } else {
            content.append("## 说明\n非 SELECT 查询未执行。\n");
        }

        long elapsed = System.currentTimeMillis() - startTime;

        return AgentResponse.builder()
                .content(content.toString())
                .metadata(Map.of(
                        "model", "dashscope",
                        "sql", sql,
                        "sqlOnly", context.isSqlOnly(),
                        "hasResult", context.getQueryResult() != null,
                        "rowCount", context.getQueryResult() != null ? context.getQueryResult().size() : 0,
                        "elapsedMs", elapsed
                ))
                .build();
    }

    /**
     * 执行 SQL（使用 MySQL 数据库或配置的数据源）。
     */
    private List<Map<String, Object>> executeSqlInternal(String sql, DataAnalysisContext context) throws SQLException {
        // 尝试从预设的数据源执行
        String jdbcUrl = properties.getJdbcUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return sqlGenerator.executeSql(conn, sql, context.getMaxRows());
        }
    }

    /**
     * 设置预设 Schema（用于测试或外部注入）。
     *
     * @param schema 预设的表结构映射
     */
    public void setPresetSchema(Map<String, DataAnalysisContext.TableSchema> schema) {
        this.presetSchema = schema;
    }

    private boolean isSelectQuery(String sql) {
        return sql != null && sql.trim().toUpperCase().startsWith("SELECT");
    }

    /**
     * 构建 Mock 数据库 Schema（MySQL 兼容）。
     */
    private Map<String, DataAnalysisContext.TableSchema> buildMockSchema() {
        Map<String, List<List<String>>> tablesDef = new LinkedHashMap<>();

        tablesDef.put("employees", List.of(
                List.of("id", "INT", "NOT NULL", "PK"),
                List.of("name", "VARCHAR(100)", "NOT NULL"),
                List.of("department", "VARCHAR(50)"),
                List.of("salary", "DECIMAL(10,2)"),
                List.of("hire_date", "DATE"),
                List.of("email", "VARCHAR(100)")
        ));

        tablesDef.put("orders", List.of(
                List.of("id", "INT", "NOT NULL", "PK"),
                List.of("customer_name", "VARCHAR(100)", "NOT NULL"),
                List.of("amount", "DECIMAL(12,2)", "NOT NULL"),
                List.of("status", "VARCHAR(20)"),
                List.of("created_at", "TIMESTAMP"),
                List.of("employee_id", "INT")
        ));

        tablesDef.put("products", List.of(
                List.of("id", "INT", "NOT NULL", "PK"),
                List.of("name", "VARCHAR(200)", "NOT NULL"),
                List.of("category", "VARCHAR(50)"),
                List.of("price", "DECIMAL(10,2)", "NOT NULL"),
                List.of("stock", "INT"),
                List.of("created_at", "TIMESTAMP")
        ));

        return schemaExtractor.buildFromPreset(tablesDef);
    }

    @Override
    public String name() {
        return "data-analysis-agent";
    }

    @Override
    protected String getOrchestratorName() {
        return "DataAnalysisAgent";
    }
}
