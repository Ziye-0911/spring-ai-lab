package com.liziye.spring.ai.lab.scenario.dataanalysis.model;

import com.liziye.spring.ai.lab.core.model.AgentContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

/**
 * 数据分析专属上下文。
 *
 * <p>扩展基类 {@link com.liziye.spring.ai.lab.core.model.AgentContext}，
 * 增加 NL2SQL 所需的参数：自然语言查询、生成的 SQL、数据库 Schema、
 * 查询结果、执行模式等。
 *
 * <p>包含内部类 {@link TableSchema} 和 {@link ColumnDef} 用于定义表结构和列信息。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataAnalysisContext extends AgentContext {

    /** 用户自然语言查询 */
    private String naturalLanguageQuery;

    /** 生成的 SQL */
    private String generatedSql;

    /** 数据库 Schema 信息 */
    private Map<String, TableSchema> tableSchemas = new LinkedHashMap<>();

    /** 查询结果（Map 列表） */
    private List<Map<String, Object>> queryResult;

    /** 是否跳过 SQL 执行（仅生成 SQL） */
    private boolean sqlOnly = false;

    /** 最大返回行数 */
    private int maxRows = 100;

    /** 查询超时时间（秒） */
    private int queryTimeoutSeconds = 30;

    /** 数据源类型 */
    private String datasourceType = "mysql";

    /**
     * 表结构定义。
     */
    @Data
    public static class TableSchema {
        private String tableName;
        private List<ColumnDef> columns = new ArrayList<>();
        private String comment;
    }

    /**
     * 列定义。
     */
    @Data
    public static class ColumnDef {
        private String columnName;
        private String dataType;
        private boolean primaryKey;
        private boolean nullable;
        private String defaultValue;
        private String comment;
    }
}
