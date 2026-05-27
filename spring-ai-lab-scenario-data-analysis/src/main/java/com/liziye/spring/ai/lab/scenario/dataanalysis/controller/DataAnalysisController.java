package com.liziye.spring.ai.lab.scenario.dataanalysis.controller;

import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.ApiResult;
import com.liziye.spring.ai.lab.scenario.dataanalysis.model.DataAnalysisContext;
import com.liziye.spring.ai.lab.scenario.dataanalysis.orchestrator.DataAnalysisAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 数据分析 REST 控制器。
 *
 * <p>提供 NL2SQL 查询、SQL 生成、Schema 查看和健康检查接口：
 * <ul>
 *   <li>POST /api/data-analysis/query — NL2SQL 查询（生成 SQL 并执行分析）</li>
 *   <li>POST /api/data-analysis/generate-sql — 仅生成 SQL（不执行）</li>
 *   <li>GET /api/data-analysis/schema — 获取可用数据库 Schema</li>
 *   <li>GET /api/data-analysis/health — 健康检查</li>
 * </ul>
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/data-analysis")
@RequiredArgsConstructor
public class DataAnalysisController {

    private final DataAnalysisAgent agent;

    /**
     * NL2SQL 查询。
     *
     * <p>接收自然语言查询，通过 {@link DataAnalysisAgent} 生成 SQL、
     * 执行查询并返回 AI 分析结果。支持 {@code sqlOnly} 参数仅生成 SQL 不执行。
     *
     * @param request 请求体，包含 {@code userInput}（自然语言查询）、
     *                {@code sqlOnly}（是否仅生成 SQL，默认为 {@code false}）
     * @return Agent 响应，包含生成的 SQL、查询结果和 AI 分析
     */
    @PostMapping("/query")
    public ApiResult<AgentResponse> query(@RequestBody Map<String, Object> request) {
        String userInput = (String) request.get("userInput");
        Boolean sqlOnly = (Boolean) request.getOrDefault("sqlOnly", false);
        String conversationId = UUID.randomUUID().toString();

        DataAnalysisContext context = new DataAnalysisContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setSqlOnly(sqlOnly != null && sqlOnly);

        log.info("[DATA-ANALYSIS] conversation={} sqlOnly={} input={}",
                conversationId, context.isSqlOnly(), userInput);

        AgentResponse response = agent.execute(conversationId, userInput, context);
        return ApiResult.success(response);
    }

    /**
     * 仅生成 SQL（不执行查询）。
     *
     * @param request 请求体，包含 {@code userInput}（自然语言查询）
     * @return Agent 响应，仅包含生成的 SQL，不包含查询结果和分析
     */
    @PostMapping("/generate-sql")
    public ApiResult<AgentResponse> generateSql(@RequestBody Map<String, Object> request) {
        String userInput = (String) request.get("userInput");
        String conversationId = UUID.randomUUID().toString();

        DataAnalysisContext context = new DataAnalysisContext();
        context.setConversationId(conversationId);
        context.setModelProvider("dashscope");
        context.setSqlOnly(true);

        AgentResponse response = agent.execute(conversationId, userInput, context);
        return ApiResult.success(response);
    }

    /**
     * 获取可用数据库 Schema。
     *
     * @return 当前可用的表结构信息（Mock 数据或实际数据库 Schema）
     */
    @GetMapping("/schema")
    public ApiResult<Map<String, Object>> schema() {
        return ApiResult.success(Map.of(
                "tables", Map.of(
                        "employees", "员工表（id, name, department, salary, hire_date, email）",
                        "orders", "订单表（id, customer_name, amount, status, created_at, employee_id）",
                        "products", "产品表（id, name, category, price, stock, created_at）"
                ),
                "note", "当前使用 Mock Schema，实际环境请配置 datasource"
        ));
    }

    /**
     * 健康检查。
     *
     * @return 服务状态（UP）、Agent 名称和可用工具列表
     */
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        return ApiResult.success(Map.of(
                "status", "UP",
                "agent", agent.name(),
                "availableTools", agent.getAvailableTools()
        ));
    }
}
