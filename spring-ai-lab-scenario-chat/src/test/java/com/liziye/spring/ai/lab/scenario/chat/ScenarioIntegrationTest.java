package com.liziye.spring.ai.lab.scenario.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

/**
 * 全场景真实 API 集成测试。
 *
 * <p>使用真实 AI API 端点测试所有 7 个场景模块 + 横切关注点。</p>
 *
 * <pre>
 * 测试覆盖:
 *   1.  Chat           — 基础对话 + 多轮记忆
 *   2.  RAG            — 知识库问答
 *   3.  Multi-Agent    — 顺序/并行协作
 *   4.  Code Review    — 代码片段 + Diff 审查
 *   5.  Data Analysis  — NL2SQL + Schema
 *   6.  Customer Svcs  — 智能客服 (咨询/投诉/反馈)
 *   7.  MCP Server     — 工具清单 + 初始化
 *   8.  Health Checks  — 5个模块健康检测
 *   9.  Resilience     — 熔断器 / 限流 / 指标
 * </pre>
 */
@SpringBootTest(
        classes = TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.location=classpath:application-test.yml"
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Spring AI Lab 全场景集成测试")
class ScenarioIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<TestReport> reports = Collections.synchronizedList(new ArrayList<>());

    // ================================================================
    // 1. Chat 基础对话
    // ================================================================

    @Test @Order(1) @DisplayName("1. Chat 基础对话")
    void testChat() {
        ResponseEntity<String> resp = post("/api/chat",
                Map.of("userInput", "你好，请用一句话介绍你自己"));
        report("Chat", "POST /api/chat", resp);
    }

    @Test @Order(2) @DisplayName("1b. Chat 多轮对话记忆")
    void testChatMultiTurn() {
        String convId = "chat-" + Uuid.short8();
        post("/api/chat", Map.of("conversationId", convId, "userInput", "我叫小明"));
        ResponseEntity<String> resp = post("/api/chat",
                Map.of("conversationId", convId, "userInput", "我叫什么名字？"));
        report("Chat-多轮", "POST /api/chat (2 turns)", resp);
    }

    // ================================================================
    // 2. RAG 知识库问答
    // ================================================================

    @Test @Order(3) @DisplayName("2. RAG 知识库问答")
    void testRagQa() {
        ResponseEntity<String> resp = post("/api/rag/ask",
                Map.of("userInput", "什么是 Spring AI？"));
        report("RAG", "POST /api/rag/ask", resp);
    }

    @Test @Order(4) @DisplayName("2b. RAG 配置查询")
    void testRagConfig() {
        report("RAG-Config", "GET /api/rag/config", get("/api/rag/config"));
    }

    // ================================================================
    // 3. Multi-Agent 多 Agent 协作
    // ================================================================

    @Test @Order(5) @DisplayName("3. Multi-Agent 顺序协作")
    void testMultiAgentSequential() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("userInput", "设计一个简单的用户注册系统");
        req.put("mode", "sequential");
        req.put("agents", List.of(
                Map.of("name", "需求分析师", "description", "分析需求",
                        "systemPrompt", "你是需求分析师，请简要分析需求给出要点。"),
                Map.of("name", "架构设计师", "description", "设计架构",
                        "systemPrompt", "你是架构师，请基于需求给出简短的架构建议。")
        ));
        report("Multi-Agent", "POST /api/multi-agent/execute (sequential)",
                post("/api/multi-agent/execute", req));
    }

    @Test @Order(6) @DisplayName("3b. Multi-Agent 并行协作")
    void testMultiAgentParallel() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("userInput", "帮我制定产品推广方案");
        req.put("mode", "parallel");
        req.put("agents", List.of(
                Map.of("name", "市场分析师", "description", "市场分析",
                        "systemPrompt", "你是市场分析师，请从市场角度简短分析。"),
                Map.of("name", "文案专家", "description", "文案创作",
                        "systemPrompt", "你是文案专家，请写一句吸引人的推广语。")
        ));
        report("Multi-Agent", "POST /api/multi-agent/execute (parallel)",
                post("/api/multi-agent/execute", req));
    }

    @Test @Order(7) @DisplayName("3c. Multi-Agent 模式查询")
    void testMultiAgentModes() {
        report("Multi-Agent-Modes", "GET /api/multi-agent/modes",
                get("/api/multi-agent/modes"));
    }

    // ================================================================
    // 4. Code Review 代码审查
    // ================================================================

    @Test @Order(8) @DisplayName("4. Code Review 代码片段审查")
    void testCodeReviewSnippet() {
        report("Code-Review", "POST /api/code-review/snippet",
                post("/api/code-review/snippet", Map.of(
                        "code", "public class Calc { public int div(int a,int b){return a/b;} }",
                        "language", "java")));
    }

    @Test @Order(9) @DisplayName("4b. Code Review Diff 审查")
    void testCodeReviewSubmit() {
        report("Code-Review", "POST /api/code-review/submit",
                post("/api/code-review/submit", Map.of(
                        "diffContent", "--- a/UserService.java\n+++ b/UserService.java\n" +
                                "-    private String password = \"\";\n" +
                                "+    private String password = \"admin123\";\n",
                        "language", "java", "criticalOnly", false)));
    }

    // ================================================================
    // 5. Data Analysis 数据分析
    // ================================================================

    @Test @Order(10) @DisplayName("5. Data Analysis NL2SQL")
    void testDataAnalysisQuery() {
        report("Data-Analysis", "POST /api/data-analysis/query",
                post("/api/data-analysis/query",
                        Map.of("userInput", "查询工资最高的前3名员工", "sqlOnly", false)));
    }

    @Test @Order(11) @DisplayName("5b. Data Analysis 仅生成SQL")
    void testDataAnalysisGenerateSql() {
        report("Data-Analysis", "POST /api/data-analysis/generate-sql",
                post("/api/data-analysis/generate-sql",
                        Map.of("userInput", "统计每个部门平均工资")));
    }

    @Test @Order(12) @DisplayName("5c. Data Analysis Schema")
    void testDataAnalysisSchema() {
        report("Data-Analysis", "GET /api/data-analysis/schema",
                get("/api/data-analysis/schema"));
    }

    // ================================================================
    // 6. Customer Service 智能客服
    // ================================================================

    @Test @Order(13) @DisplayName("6. Customer Service 咨询")
    void testCustomerServiceInquiry() {
        report("Customer-Service", "POST /api/cs/chat (咨询)",
                post("/api/cs/chat", Map.of(
                        "conversationId", "cs-" + Uuid.short8(),
                        "userInput", "请问你们的售后服务时间是多久？")));
    }

    @Test @Order(14) @DisplayName("6b. Customer Service 投诉")
    void testCustomerServiceComplaint() {
        String cid = "cs-comp-" + Uuid.short8();
        ResponseEntity<String> resp = post("/api/cs/chat", Map.of(
                "conversationId", cid,
                "userInput", "我对你们的产品质量非常不满意，要投诉！"));
        report("Customer-Service", "POST /api/cs/chat (投诉)", resp);

        report("CS-Session", "GET /api/cs/session/{id}/count",
                get("/api/cs/session/" + cid + "/count"));
    }

    @Test @Order(15) @DisplayName("6c. Customer Service 反馈")
    void testCustomerServiceFeedback() {
        report("Customer-Service", "POST /api/cs/chat (反馈)",
                post("/api/cs/chat", Map.of(
                        "conversationId", "cs-fb-" + Uuid.short8(),
                        "userInput", "你好，我想反馈一个产品建议")));
    }

    // ================================================================
    // 7. MCP Server
    // ================================================================

    @Test @Order(16) @DisplayName("7. MCP Server 工具清单")
    void testMcpToolList() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("jsonrpc", "2.0");
        req.put("id", 1);
        req.put("method", "tools/list");
        req.put("params", Map.of());
        report("MCP", "POST /mcp/sse/message?sessionId=test (tools/list)",
                post("/mcp/sse/message?sessionId=mcp-test", req));
    }

    @Test @Order(17) @DisplayName("7b. MCP Server 初始化")
    void testMcpInitialize() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("jsonrpc", "2.0");
        req.put("id", 2);
        req.put("method", "initialize");
        req.put("params", Map.of(
                "protocolVersion", "2024-11-05",
                "clientInfo", Map.of("name", "test-client", "version", "1.0")));
        report("MCP", "POST /mcp/sse/message?sessionId=mcp-2 (initialize)",
                post("/mcp/sse/message?sessionId=mcp-init", req));
    }

    @Test @Order(18) @DisplayName("7c. MCP 活跃会话")
    void testMcpSessionsCount() {
        report("MCP-Sessions", "GET /mcp/sse/sessions/count",
                get("/mcp/sse/sessions/count"));
    }

    // ================================================================
    // Health 健康检测
    // ================================================================

    @Test @Order(20) @DisplayName("Health: Chat")
    void testHealthChat() {
        report("Health", "GET /api/chat/health", get("/api/chat/health"));
    }

    @Test @Order(21) @DisplayName("Health: Multi-Agent")
    void testHealthMultiAgent() {
        report("Health", "GET /api/multi-agent/health", get("/api/multi-agent/health"));
    }

    @Test @Order(22) @DisplayName("Health: Code Review")
    void testHealthCodeReview() {
        report("Health", "GET /api/code-review/health", get("/api/code-review/health"));
    }

    @Test @Order(23) @DisplayName("Health: Data Analysis")
    void testHealthDataAnalysis() {
        report("Health", "GET /api/data-analysis/health", get("/api/data-analysis/health"));
    }

    // ================================================================
    // X. 横切关注点：RateLimiter + 熔断器 + 指标
    // ================================================================

    @Test @Order(30) @DisplayName("X1. RateLimiter 令牌桶")
    void testRateLimiter() {
        // RateLimiter(permitsPerSecond): 100qps，15次请求应全部通过
        com.liziye.spring.ai.lab.core.security.RateLimiter limiter =
                new com.liziye.spring.ai.lab.core.security.RateLimiter(100.0);

        int allowed = 0;
        for (int i = 0; i < 15; i++) {
            if (limiter.tryAcquire("test-key")) allowed++;
        }

        boolean pass = allowed == 15;
        reportRaw("RateLimiter", "令牌桶 (100qps)", pass,
                String.format("15次请求全部通过: %s", pass ? "是" : "否(通过" + allowed + ")"));
        if (!pass) Assertions.fail("RateLimiter 15次请求应全部允许，实际:" + allowed);
    }

    @Test @Order(31) @DisplayName("X2. CircuitBreaker 熔断器")
    void testCircuitBreaker() {
        com.liziye.spring.ai.lab.core.resilience.CircuitBreakerManager cb =
                new com.liziye.spring.ai.lab.core.resilience.CircuitBreakerManager(
                        new com.liziye.spring.ai.lab.core.resilience.CircuitBreakerManager.CircuitBreakerConfig());
        String state = cb.getState("test-cb").name();
        boolean pass = "CLOSED".equals(state);
        reportRaw("CircuitBreaker", "初始状态", pass, "状态: " + state);
        if (!pass) Assertions.fail("熔断器初始状态不是 CLOSED: " + state);
    }

    @Test @Order(32) @DisplayName("X3. Token/Latency 指标收集")
    void testMetricsTokenLatency() {
        var tokenMetrics = new com.liziye.spring.ai.lab.core.observation.TokenMetrics();
        var latencyMetrics = new com.liziye.spring.ai.lab.core.observation.LatencyMetrics();

        long now = System.currentTimeMillis();
        tokenMetrics.recordUsage("test-model", 300);
        tokenMetrics.recordUsage("test-model", 200);
        latencyMetrics.recordLatency("test-model", System.currentTimeMillis() - now);
        latencyMetrics.recordLatency("test-model", 10);

        long total = tokenMetrics.getTotalTokens();
        long modelTokens = tokenMetrics.getTokensByModel("test-model");
        double avgLat = latencyMetrics.getAverageLatency("test-model");

        boolean pass = total == 500 && modelTokens == 500 && avgLat >= 0;
        reportRaw("Metrics", "Token/Latency", pass,
                String.format("Tokens=%d, AvgLat=%.1fms", total, avgLat));
        if (!pass) Assertions.fail(String.format("指标异常: tokens=%d, latency=%.1f", total, avgLat));
    }

    @Test @Order(33) @DisplayName("X4. DocumentMetrics 文档指标")
    void testDocumentMetrics() {
        var m = new com.liziye.spring.ai.lab.core.observation.DocumentMetrics();
        m.recordDocumentLoaded(true);
        m.recordDocumentLoaded(true);
        m.recordDocumentLoaded(false);
        m.recordVectorStored(true);
        m.recordVectorStored(true);
        m.recordVectorStored(true);

        boolean pass = m.getDocumentsLoadedSuccess() == 2
                && m.getDocumentsLoadedFailed() == 1
                && m.getVectorsStoredSuccess() == 3
                && m.getDocumentLoadSuccessRate() > 0.6;
        reportRaw("Metrics", "文档处理指标", pass,
                String.format("Loaded=%d, Failed=%d, Vectors=%d",
                        m.getDocumentsLoadedSuccess(), m.getDocumentsLoadedFailed(),
                        m.getVectorsStoredSuccess()));
        if (!pass) Assertions.fail("DocumentMetrics 值不符合预期");
    }

    @Test @Order(34) @DisplayName("X5. ErrorMetrics 错误统计")
    void testErrorMetrics() {
        var m = new com.liziye.spring.ai.lab.core.observation.ErrorMetrics();
        m.recordError("TimeoutException");
        m.recordError("TimeoutException");
        m.recordError("RateLimitExceeded");

        boolean pass = m.getErrorCount("TimeoutException") == 2
                && m.getErrorCount("RateLimitExceeded") == 1
                && m.getTotalErrors() == 3;
        reportRaw("Metrics", "错误统计", pass,
                String.format("Timeout=%d, RateLimit=%d, Total=%d",
                        m.getErrorCount("TimeoutException"),
                        m.getErrorCount("RateLimitExceeded"), m.getTotalErrors()));
        if (!pass) Assertions.fail("ErrorMetrics 值不符合预期");
    }

    // ================================================================
    // 报告汇总
    // ================================================================

    @AfterAll
    static void printSummary() {
        long passed = reports.stream().filter(r -> r.passed).count();
        long failed = reports.size() - passed;

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("        Spring AI Lab 全场景功能测试报告");
        System.out.println("═══════════════════════════════════════════════════════════════");

        String cat = "";
        for (TestReport r : reports) {
            if (!r.category.equals(cat)) {
                cat = r.category;
                System.out.printf("%n  ── %s ──%n", cat);
            }
            System.out.printf("  %s %s%n", r.passed ? "✅" : "❌", r.testName);
        }

        System.out.println();
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.printf("  总计: %d | ✅ %d | ❌ %d%n", reports.size(), passed, failed);
        System.out.println("───────────────────────────────────────────────────────────────");
        if (failed == 0) System.out.println("  🎉 全部通过！");
        else System.out.println("  ⚠️  有失败项，请排查。");
        System.out.println();
    }

    // ================================================================
    // 辅助
    // ================================================================

    private ResponseEntity<String> post(String path, Object body) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            return restTemplate.postForEntity(url(path),
                    new HttpEntity<>(mapper.writeValueAsString(body), h), String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private ResponseEntity<String> get(String path) {
        try {
            return restTemplate.getForEntity(url(path), String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void report(String cat, String name, ResponseEntity<String> resp) {
        boolean ok = resp.getStatusCode().is2xxSuccessful();
        String detail = ok
                ? "HTTP " + resp.getStatusCode().value() + " → "
                + trunc(resp.getBody(), 80)
                : "HTTP " + resp.getStatusCode().value() + ": "
                + trunc(resp.getBody(), 80);

        // 检查业务响应码
        if (ok && resp.getBody() != null && resp.getBody().contains("\"code\"")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = mapper.readValue(resp.getBody(), Map.class);
                Object code = m.get("code");
                if (code instanceof Integer && (Integer) code != 200) {
                    ok = false;
                    detail = String.format("业务码=%d: %s", code, m.getOrDefault("message", ""));
                }
            } catch (Exception ignored) {}
        }

        reports.add(new TestReport(cat, name, ok, detail));
        System.out.printf("  %s [%s] %s — %s%n", ok ? "✅" : "❌", cat, name, detail);
    }

    private void reportRaw(String cat, String name, boolean pass, String detail) {
        reports.add(new TestReport(cat, name, pass, pass ? detail : "FAIL: " + detail));
        System.out.printf("  %s [%s] %s — %s%n", pass ? "✅" : "❌", cat, name, detail);
    }

    private static String trunc(String s, int n) {
        if (s == null) return "null";
        String clean = s.replace("\n", " ").replace("\r", "");
        return clean.length() > n ? clean.substring(0, n) + "…" : clean;
    }

    /** 便捷 UUID 短码 */
    static class Uuid {
        static String short8() {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    static class TestReport {
        final String category;
        final String testName;
        final boolean passed;
        final String detail;

        TestReport(String c, String n, boolean p, String d) {
            this.category = c; this.testName = n; this.passed = p; this.detail = d;
        }
    }
}
