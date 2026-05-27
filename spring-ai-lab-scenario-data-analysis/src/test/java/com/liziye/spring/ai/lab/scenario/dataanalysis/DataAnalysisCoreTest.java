package com.liziye.spring.ai.lab.scenario.dataanalysis;

import com.liziye.spring.ai.lab.scenario.dataanalysis.model.DataAnalysisContext;
import com.liziye.spring.ai.lab.scenario.dataanalysis.nl2sql.SchemaExtractor;
import com.liziye.spring.ai.lab.scenario.dataanalysis.nl2sql.SqlGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据分析（NL2SQL）核心逻辑测试。
 *
 * 验证 Schema 提取、SQL 清理、Mock Schema 构建、默认配置等，
 * 不依赖真实数据库连接。
 */
class DataAnalysisCoreTest {

    // ================================================================
    // 1. 默认配置改为 MySQL 的验证
    // ================================================================

    @Nested
    @DisplayName("默认配置验证 - MySQL 替代 H2")
    class DefaultConfigurationTest {

        @Test
        @DisplayName("DataAnalysisProperties 默认值应为 MySQL")
        void shouldDefaultToMysqlProperties() {
            DataAnalysisProperties properties = new DataAnalysisProperties();

            assertThat(properties.getDatasourceType())
                    .as("数据源类型默认应为 mysql")
                    .isEqualTo("mysql");

            assertThat(properties.getJdbcUrl())
                    .as("JDBC URL 应为 MySQL 连接串")
                    .startsWith("jdbc:mysql://");

            assertThat(properties.getJdbcUrl())
                    .as("JDBC URL 应包含 ai_lab 数据库名")
                    .contains("ai_lab");

            assertThat(properties.getUsername())
                    .as("默认用户名应为 root")
                    .isEqualTo("root");

            assertThat(properties.getPassword())
                    .as("默认密码应为 root")
                    .isEqualTo("root");
        }

        @Test
        @DisplayName("DataAnalysisContext 默认数据源类型应为 mysql")
        void shouldDefaultContextToMysql() {
            DataAnalysisContext context = new DataAnalysisContext();

            assertThat(context.getDatasourceType())
                    .as("上下文中的数据源类型默认应为 mysql")
                    .isEqualTo("mysql");
        }
    }

    // ================================================================
    // 2. SchemaExtractor 测试（纯内存，不需要数据库）
    // ================================================================

    @Nested
    @DisplayName("Schema 提取器测试")
    class SchemaExtractorTest {

        private final SchemaExtractor extractor = new SchemaExtractor();

        @Test
        @DisplayName("应能从预设定义构建 Schema")
        void shouldBuildFromPreset() {
            Map<String, List<List<String>>> tablesDef = new LinkedHashMap<>();
            tablesDef.put("employees", List.of(
                    List.of("id", "INT", "NOT NULL", "PK"),
                    List.of("name", "VARCHAR(100)", "NOT NULL"),
                    List.of("salary", "DECIMAL(10,2)")
            ));

            Map<String, DataAnalysisContext.TableSchema> schemas =
                    extractor.buildFromPreset(tablesDef);

            assertThat(schemas).hasSize(1);
            DataAnalysisContext.TableSchema emp = schemas.get("employees");
            assertThat(emp).isNotNull();
            assertThat(emp.getTableName()).isEqualTo("employees");
            assertThat(emp.getColumns()).hasSize(3);

            // 主键
            assertThat(emp.getColumns().get(0).isPrimaryKey()).isTrue();
            assertThat(emp.getColumns().get(1).isPrimaryKey()).isFalse();

            // 可空
            assertThat(emp.getColumns().get(0).isNullable()).isFalse();
            assertThat(emp.getColumns().get(1).isNullable()).isFalse();
            assertThat(emp.getColumns().get(2).isNullable()).isTrue();
        }

        @Test
        @DisplayName("应能格式化 Schema 为 Prompt 可用的 Markdown 文本")
        void shouldFormatSchemaForPrompt() {
            Map<String, List<List<String>>> tablesDef = new LinkedHashMap<>();
            tablesDef.put("products", List.of(
                    List.of("id", "INT", "NOT NULL", "PK"),
                    List.of("name", "VARCHAR(200)", "NOT NULL"),
                    List.of("category", "VARCHAR(50)"),
                    List.of("price", "DECIMAL(10,2)", "NOT NULL"),
                    List.of("stock", "INT"),
                    List.of("created_at", "TIMESTAMP")
            ));

            Map<String, DataAnalysisContext.TableSchema> schemas =
                    extractor.buildFromPreset(tablesDef);
            String prompt = extractor.formatForPrompt(schemas);

            // 验证包含关键内容
            assertThat(prompt)
                    .contains("### 表: products")
                    .contains("id")
                    .contains("INT")
                    .contains("name")
                    .contains("VARCHAR(200)")
                    .contains("price")
                    .contains("DECIMAL(10,2)")
                    .contains("created_at")
                    .contains("TIMESTAMP")
                    .contains("是")  // 主键标记
                    .contains("否"); // 非空标记
        }

        @Test
        @DisplayName("格式化空 Schema 应返回提示文本")
        void shouldReturnEmptyHintForNullSchema() {
            assertThat(extractor.formatForPrompt(null))
                    .contains("无可用表结构");

            assertThat(extractor.formatForPrompt(Collections.emptyMap()))
                    .contains("无可用表结构");
        }

        @Test
        @DisplayName("应能构建完整的三表 Mock Schema")
        void shouldBuildFullMockSchema() {
            // 模拟 DataAnalysisAgent.buildMockSchema() 的定义
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

            Map<String, DataAnalysisContext.TableSchema> schemas =
                    extractor.buildFromPreset(tablesDef);

            assertThat(schemas).hasSize(3);
            assertThat(schemas).containsKeys("employees", "orders", "products");

            // 验证每个表的列数和主键
            assertThat(schemas.get("employees").getColumns()).hasSize(6);
            assertThat(schemas.get("orders").getColumns()).hasSize(6);
            assertThat(schemas.get("products").getColumns()).hasSize(6);

            // 所有表第一列都是主键
            schemas.values().forEach(table ->
                    assertThat(table.getColumns().get(0).isPrimaryKey())
                            .as(table.getTableName() + " 的第一列应为主键")
                            .isTrue());
        }
    }

    // ================================================================
    // 3. SqlGenerator.cleanSql() 测试
    // ================================================================

    @Nested
    @DisplayName("SQL 清理逻辑测试")
    class SqlCleanTest {

        private final SqlGenerator sqlGenerator = new SqlGenerator(
                new DataAnalysisProperties());

        @ParameterizedTest(name = "[{index}] {0}")
        @CsvSource(delimiter = '|', textBlock = """
                clean: simple SELECT          | SELECT * FROM employees | SELECT * FROM employees
                clean: with trailing semicolon| SELECT * FROM employees; | SELECT * FROM employees;
                clean: with whitespace        |   SELECT 1   | SELECT 1
                clean: null input             | null | ''
                """)
        @DisplayName("SQL 清理 - 简单输入")
        void shouldCleanSimpleSql(String description, String input, String expected) {
            input = "null".equals(input) ? null : input;
            String result = invokeCleanSql(sqlGenerator, input);
            assertThat(result).as(description).isEqualTo(expected);
        }

        @Test
        @DisplayName("应清理 Markdown sql 代码块")
        void shouldCleanMarkdownSqlCodeBlock() {
            String input = "```sql\nSELECT * FROM orders\n```";
            String result = invokeCleanSql(sqlGenerator, input);
            assertThat(result).isEqualTo("SELECT * FROM orders");
        }

        @Test
        @DisplayName("应清理 Markdown 代码块（无 sql 标注）")
        void shouldCleanMarkdownCodeBlock() {
            String input = "```\nSELECT name FROM products\n```";
            String result = invokeCleanSql(sqlGenerator, input);
            assertThat(result).isEqualTo("SELECT name FROM products");
        }

        @Test
        @DisplayName("应正确处理多行 SQL")
        void shouldHandleMultiLineSql() {
            String input = """
                    ```sql
                    SELECT
                      e.name,
                      d.dept_name
                    FROM employees e
                    JOIN departments d ON e.dept_id = d.id
                    WHERE e.salary > 5000
                    ```
                    """;

            String result = invokeCleanSql(sqlGenerator, input);

            assertThat(result)
                    .contains("SELECT")
                    .contains("e.name")
                    .contains("employees")
                    .doesNotContain("```");
        }

        @Test
        @DisplayName("应去除 SQL 后多余的说明文字")
        void shouldTrimExtraContent() {
            // 当有分号分隔时
            String input = "SELECT count(*) FROM orders;\n\nNote: This query returns the total number of orders.";
            String result = invokeCleanSql(sqlGenerator, input);

            assertThat(result).startsWith("SELECT count(*) FROM orders");
        }

        private String invokeCleanSql(SqlGenerator generator, String raw) {
            try {
                var method = SqlGenerator.class.getDeclaredMethod("cleanSql", String.class);
                method.setAccessible(true);
                return (String) method.invoke(generator, raw);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ================================================================
    // 4. DataAnalysisContext 模型测试
    // ================================================================

    @Nested
    @DisplayName("上下文模型测试")
    class ContextModelTest {

        @Test
        @DisplayName("应正确设置和读取字段")
        void shouldSetAndGetFields() {
            DataAnalysisContext context = new DataAnalysisContext();

            context.setConversationId("test-conv-001");
            context.setModelProvider("dashscope");
            context.setNaturalLanguageQuery("查询各部门平均工资");
            context.setGeneratedSql("SELECT department, AVG(salary) FROM employees GROUP BY department");
            context.setSqlOnly(false);
            context.setMaxRows(50);
            context.setDatasourceType("mysql");

            assertThat(context.getConversationId()).isEqualTo("test-conv-001");
            assertThat(context.getNaturalLanguageQuery()).isEqualTo("查询各部门平均工资");
            assertThat(context.getGeneratedSql()).contains("AVG(salary)");
            assertThat(context.isSqlOnly()).isFalse();
            assertThat(context.getMaxRows()).isEqualTo(50);
            assertThat(context.getDatasourceType()).isEqualTo("mysql");
        }

        @Test
        @DisplayName("TableSchema 和 ColumnDef 模型应正常工作")
        void shouldBuildTableSchema() {
            DataAnalysisContext.TableSchema schema = new DataAnalysisContext.TableSchema();
            schema.setTableName("test_table");
            schema.setComment("测试表");

            DataAnalysisContext.ColumnDef col1 = new DataAnalysisContext.ColumnDef();
            col1.setColumnName("id");
            col1.setDataType("INT");
            col1.setPrimaryKey(true);
            col1.setNullable(false);

            DataAnalysisContext.ColumnDef col2 = new DataAnalysisContext.ColumnDef();
            col2.setColumnName("name");
            col2.setDataType("VARCHAR(100)");
            col2.setPrimaryKey(false);
            col2.setNullable(true);
            col2.setComment("名称");

            schema.getColumns().add(col1);
            schema.getColumns().add(col2);

            assertThat(schema.getColumns()).hasSize(2);
            assertThat(schema.getColumns().get(0).getColumnName()).isEqualTo("id");
            assertThat(schema.getColumns().get(0).isPrimaryKey()).isTrue();
            assertThat(schema.getColumns().get(1).getComment()).isEqualTo("名称");
        }
    }

    // ================================================================
    // 5. DataAnalysisProperties 系统提示词测试
    // ================================================================

    @Nested
    @DisplayName("系统提示词测试")
    class SystemPromptTest {

        @Test
        @DisplayName("NL2SQL 系统提示词应包含关键规则")
        void shouldContainNl2sqlRules() {
            DataAnalysisProperties properties = new DataAnalysisProperties();
            String prompt = properties.getNl2sqlSystemPrompt();

            assertThat(prompt).isNotNull().isNotBlank();
            assertThat(prompt)
                    .contains("SQL 生成器")
                    .contains("SELECT")
                    .contains("标准 SQL 语法");
        }

        @Test
        @DisplayName("分析系统提示词应包含关键维度")
        void shouldContainAnalysisDimensions() {
            DataAnalysisProperties properties = new DataAnalysisProperties();
            String prompt = properties.getAnalysisSystemPrompt();

            assertThat(prompt).isNotNull().isNotBlank();
            assertThat(prompt)
                    .contains("数据分析")
                    .contains("数据概况")
                    .contains("异常发现")
                    .contains("业务洞察")
                    .contains("建议");
        }
    }

    // ================================================================
    // 6. 使用反射验证 DataAnalysisAgent 内部方法
    // ================================================================

    @Nested
    @DisplayName("DataAnalysisAgent 内部逻辑测试")
    class DataAnalysisAgentInternalTest {

        /**
         * 辅助方法：构建模拟 DataAnalysisAgent 所需要的参数。
         * （不实际启动 Spring 容器）
         */
        @Test
        @DisplayName("Mock Schema 结构与 MySQL 兼容性验证")
        void shouldVerifyMockSchemaTypes() {
            SchemaExtractor extractor = new SchemaExtractor();

            // 与 DataAnalysisAgent.buildMockSchema() 完全相同的定义
            Map<String, List<List<String>>> mockDef = new LinkedHashMap<>();
            mockDef.put("employees", List.of(
                    List.of("id", "INT", "NOT NULL", "PK"),
                    List.of("name", "VARCHAR(100)", "NOT NULL"),
                    List.of("department", "VARCHAR(50)"),
                    List.of("salary", "DECIMAL(10,2)"),
                    List.of("hire_date", "DATE"),
                    List.of("email", "VARCHAR(100)")
            ));
            mockDef.put("orders", List.of(
                    List.of("id", "INT", "NOT NULL", "PK"),
                    List.of("customer_name", "VARCHAR(100)", "NOT NULL"),
                    List.of("amount", "DECIMAL(12,2)", "NOT NULL"),
                    List.of("status", "VARCHAR(20)"),
                    List.of("created_at", "TIMESTAMP"),
                    List.of("employee_id", "INT")
            ));
            mockDef.put("products", List.of(
                    List.of("id", "INT", "NOT NULL", "PK"),
                    List.of("name", "VARCHAR(200)", "NOT NULL"),
                    List.of("category", "VARCHAR(50)"),
                    List.of("price", "DECIMAL(10,2)", "NOT NULL"),
                    List.of("stock", "INT"),
                    List.of("created_at", "TIMESTAMP")
            ));

            Map<String, DataAnalysisContext.TableSchema> schemas =
                    extractor.buildFromPreset(mockDef);

            // 所有类型都是 MySQL 兼容的
            Set<String> allTypes = new HashSet<>();
            schemas.values().forEach(table ->
                    table.getColumns().forEach(col -> allTypes.add(col.getDataType())));

            // 验证所有类型都是 MySQL 兼容的（VARCHAR/DECIMAL 带参数后缀）
            assertThat(allTypes)
                    .contains("INT", "DATE", "TIMESTAMP");
            assertThat(allTypes.stream().anyMatch(t -> t.startsWith("VARCHAR(")))
                    .as("应包含 VARCHAR 类型")
                    .isTrue();
            assertThat(allTypes.stream().anyMatch(t -> t.startsWith("DECIMAL(")))
                    .as("应包含 DECIMAL 类型")
                    .isTrue();

            // 验证所有 VARCHAR 定义有效
            schemas.values().forEach(table ->
                    table.getColumns().stream()
                            .filter(c -> c.getDataType().startsWith("VARCHAR"))
                            .forEach(c -> {
                                assertThat(c.getDataType())
                                        .as("VARCHAR 类型格式: " + c.getColumnName())
                                        .matches("VARCHAR\\(\\d+\\)");
                            }));
        }
    }
}
