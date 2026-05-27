package com.liziye.spring.ai.lab.scenario.dataanalysis.nl2sql;

import com.liziye.spring.ai.lab.scenario.dataanalysis.model.DataAnalysisContext;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * 数据库 Schema 提取器。
 *
 * <p>从 JDBC 数据源中自动提取表结构信息（表名、列名、类型、主键等），
 * 构建 Schema 上下文供 NL2SQL 使用。
 *
 * <p>支持两种模式：
 * <ul>
 *   <li>从 JDBC 连接自动提取（{@link #extract(Connection, String, String)}）</li>
 *   <li>从预设定义手动构建（{@link #buildFromPreset(Map)}），适用于无数据库连接的 Mock 模式</li>
 * </ul>
 *
 * <p>提取的 Schema 可通过 {@link #formatForPrompt(Map)} 格式化为 LLM Prompt 可用的 Markdown 表格。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class SchemaExtractor {

    /**
     * 从 JDBC 连接中提取所有表的 Schema。
     *
     * @param connection JDBC 连接
     * @param catalog    数据库名（可为 null）
     * @param schemaPattern Schema 模式（可为 null）
     * @return 表结构映射
     */
    public Map<String, DataAnalysisContext.TableSchema> extract(Connection connection,
                                                                  String catalog,
                                                                  String schemaPattern) throws SQLException {
        Map<String, DataAnalysisContext.TableSchema> schemas = new LinkedHashMap<>();

        DatabaseMetaData metaData = connection.getMetaData();

        // 1. 获取所有表
        try (ResultSet tables = metaData.getTables(catalog, schemaPattern, "%", new String[]{"TABLE", "VIEW"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                DataAnalysisContext.TableSchema schema = new DataAnalysisContext.TableSchema();
                schema.setTableName(tableName);
                schema.setComment(tables.getString("REMARKS"));
                schemas.put(tableName, schema);
            }
        }

        // 2. 获取每个表的列信息
        for (DataAnalysisContext.TableSchema schema : schemas.values()) {
            try (ResultSet columns = metaData.getColumns(catalog, schemaPattern, schema.getTableName(), "%")) {
                while (columns.next()) {
                    DataAnalysisContext.ColumnDef column = new DataAnalysisContext.ColumnDef();
                    column.setColumnName(columns.getString("COLUMN_NAME"));
                    column.setDataType(columns.getString("TYPE_NAME"));
                    column.setNullable("YES".equals(columns.getString("IS_NULLABLE")));
                    column.setDefaultValue(columns.getString("COLUMN_DEF"));
                    column.setComment(columns.getString("REMARKS"));
                    schema.getColumns().add(column);
                }
            }

            // 3. 获取主键信息
            try (ResultSet pks = metaData.getPrimaryKeys(catalog, schemaPattern, schema.getTableName())) {
                Set<String> pkColumns = new HashSet<>();
                while (pks.next()) {
                    pkColumns.add(pks.getString("COLUMN_NAME"));
                }
                for (DataAnalysisContext.ColumnDef column : schema.getColumns()) {
                    column.setPrimaryKey(pkColumns.contains(column.getColumnName()));
                }
            }
        }

        log.info("[SchemaExtractor] Extracted {} tables: {}",
                schemas.size(),
                schemas.keySet());

        return schemas;
    }

    /**
     * 从预设表结构构建 Schema（用于无数据库连接的 Mock 模式）。
     *
     * @param tablesDef 表定义，键为表名，值为列定义列表
     *                  （每列格式：[列名, 类型, 可空?, 主键?]）
     * @return 构建的表结构映射
     */
    public Map<String, DataAnalysisContext.TableSchema> buildFromPreset(Map<String, List<List<String>>> tablesDef) {
        Map<String, DataAnalysisContext.TableSchema> schemas = new LinkedHashMap<>();

        for (Map.Entry<String, List<List<String>>> entry : tablesDef.entrySet()) {
            DataAnalysisContext.TableSchema schema = new DataAnalysisContext.TableSchema();
            schema.setTableName(entry.getKey());

            for (List<String> colDef : entry.getValue()) {
                DataAnalysisContext.ColumnDef column = new DataAnalysisContext.ColumnDef();
                column.setColumnName(colDef.get(0));
                column.setDataType(colDef.size() > 1 ? colDef.get(1) : "VARCHAR");
                column.setNullable(colDef.size() <= 2 || !"NOT NULL".equalsIgnoreCase(colDef.get(2)));
                column.setPrimaryKey(colDef.size() > 3 && "PK".equalsIgnoreCase(colDef.get(3)));
                schema.getColumns().add(column);
            }

            schemas.put(entry.getKey(), schema);
        }

        return schemas;
    }

    /**
     * 将提取的 Schema 格式化为 LLM Prompt 可用的文本。
     *
     * @param schemas 表结构映射
     * @return Markdown 表格格式的 Schema 文本，无表时返回提示信息
     */
    public String formatForPrompt(Map<String, DataAnalysisContext.TableSchema> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return "（无可用表结构）";
        }

        StringBuilder sb = new StringBuilder();

        for (DataAnalysisContext.TableSchema schema : schemas.values()) {
            sb.append("### 表: ").append(schema.getTableName()).append("\n");
            if (schema.getComment() != null && !schema.getComment().isEmpty()) {
                sb.append("说明: ").append(schema.getComment()).append("\n");
            }

            sb.append("| 列名 | 类型 | 主键 | 可空 | 说明 |\n");
            sb.append("|------|------|------|------|------|\n");

            for (DataAnalysisContext.ColumnDef col : schema.getColumns()) {
                sb.append("| ").append(col.getColumnName())
                        .append(" | ").append(col.getDataType())
                        .append(" | ").append(col.isPrimaryKey() ? "是" : "")
                        .append(" | ").append(col.isNullable() ? "是" : "否")
                        .append(" | ").append(col.getComment() != null ? col.getComment() : "")
                        .append(" |\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
