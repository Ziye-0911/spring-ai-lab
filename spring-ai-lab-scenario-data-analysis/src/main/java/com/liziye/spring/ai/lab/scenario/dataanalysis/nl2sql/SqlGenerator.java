package com.liziye.spring.ai.lab.scenario.dataanalysis.nl2sql;

import com.liziye.spring.ai.lab.scenario.dataanalysis.DataAnalysisProperties;
import org.springframework.ai.chat.client.ChatClient;

import java.sql.*;
import java.util.*;

/**
 * NL2SQL 生成器。
 *
 * <p>将自然语言查询转为 SQL，并支持执行和结果分析。
 * 核心能力：
 * <ul>
 *   <li>生成 SQL（{@link #generateSql(ChatClient, String, String)}）</li>
 *   <li>执行 SQL（{@link #executeSql(Connection, String, int)}）</li>
 *   <li>结果格式化（{@link #formatResults(List)}）</li>
 *   <li>AI 分析结果（{@link #analyzeResults(ChatClient, String, String, String)}）</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
public class SqlGenerator {

    private final DataAnalysisProperties properties;

    public SqlGenerator(DataAnalysisProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成 SQL。
     *
     * @param chatClient  ChatClient
     * @param userQuery   用户自然语言查询
     * @param schemaText  格式化的 Schema 文本
     * @return 生成的 SQL
     */
    public String generateSql(ChatClient chatClient, String userQuery, String schemaText) {
        String prompt = buildSqlGenerationPrompt(userQuery, schemaText);

        String rawResult = chatClient.prompt()
                .system(properties.getNl2sqlSystemPrompt())
                .user(prompt)
                .call()
                .content();

        return cleanSql(rawResult);
    }

    /**
     * 在数据库连接上执行 SQL。
     *
     * @param connection JDBC 连接
     * @param sql        SQL 语句
     * @param maxRows    最大返回行数
     * @return 查询结果
     */
    public List<Map<String, Object>> executeSql(Connection connection,
                                                  String sql,
                                                  int maxRows) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Statement stmt = connection.createStatement()) {
            stmt.setMaxRows(maxRows);
            stmt.setQueryTimeout(properties.getQueryTimeoutSeconds());

            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }

        return results;
    }

    /**
     * 格式化查询结果为文本。
     *
     * @param results 查询结果行列表
     * @return Markdown 表格格式的结果文本，最多显示 20 行
     */
    public String formatResults(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "（查询结果为空）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询返回 ").append(results.size()).append(" 行数据：\n\n");

        // 表头
        Set<String> columns = results.get(0).keySet();
        sb.append("| ").append(String.join(" | ", columns)).append(" |\n");
        sb.append("|").append(String.join("|", Collections.nCopies(columns.size(), "------"))).append("|\n");

        // 数据行（最多显示 20 行）
        int displayCount = Math.min(results.size(), 20);
        for (int i = 0; i < displayCount; i++) {
            Map<String, Object> row = results.get(i);
            sb.append("| ");
            boolean first = true;
            for (String col : columns) {
                if (!first) sb.append(" | ");
                Object val = row.get(col);
                sb.append(val != null ? val.toString() : "NULL");
                first = false;
            }
            sb.append(" |\n");
        }

        if (results.size() > 20) {
            sb.append("\n... 还有 ").append(results.size() - 20).append(" 行数据未显示\n");
        }

        return sb.toString();
    }

    /**
     * 对查询结果进行 AI 分析。
     *
     * @param chatClient  用于调用 LLM 的 ChatClient
     * @param userQuery   用户原始自然语言查询
     * @param sql         执行的 SQL 语句
     * @param resultsText 格式化的查询结果文本
     * @return AI 分析结果
     */
    public String analyzeResults(ChatClient chatClient,
                                  String userQuery,
                                  String sql,
                                  String resultsText) {
        String prompt = "请对以下 SQL 查询结果进行数据分析：\n\n"
                + "### 用户原始问题\n" + userQuery + "\n\n"
                + "### 执行的 SQL\n```sql\n" + sql + "\n```\n\n"
                + "### 查询结果\n" + resultsText;

        return chatClient.prompt()
                .system(properties.getAnalysisSystemPrompt())
                .user(prompt)
                .call()
                .content();
    }

    private String buildSqlGenerationPrompt(String userQuery, String schemaText) {
        return "请根据用户的自然语言描述和提供的数据库表结构，生成 SQL 查询语句。\n\n"
                + "### 数据库表结构\n" + schemaText + "\n\n"
                + "### 用户查询\n" + userQuery + "\n\n"
                + "请生成 SQL 语句：";
    }

    /**
     * 清理 LLM 输出中的 SQL（去除 Markdown 标记和多余的空白）。
     */
    private String cleanSql(String raw) {
        if (raw == null) return "";

        // 去除 Markdown 代码块标记
        String sql = raw
                .replaceAll("```sql\\s*", "")
                .replaceAll("```\\s*$", "")
                .replaceAll("^```\\s*", "")
                .trim();

        // 去除分号后的多余内容
        int semicolonIndex = sql.lastIndexOf(';');
        if (semicolonIndex > 0 && semicolonIndex < sql.length() - 1) {
            String afterSemicolon = sql.substring(semicolonIndex + 1).trim();
            if (!afterSemicolon.isEmpty() && !afterSemicolon.startsWith("--")) {
                sql = sql.substring(0, semicolonIndex + 1);
            }
        }

        return sql.trim();
    }
}
