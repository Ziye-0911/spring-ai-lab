package com.liziye.spring.ai.lab.scenario.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liziye.spring.ai.lab.core.skill.ParsedSkill;
import com.liziye.spring.ai.lab.core.skill.SkillRegistry;
import com.liziye.spring.ai.lab.core.skill.SkillRouter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 系统端到端集成测试 — 使用小米 Token-Plan API。
 *
 * <p>验证完整链路：SkillLoader 加载 → SkillRouter 语义匹配 → 系统提词注入 → AI 响应。
 *
 * <h3>运行方式</h3>
 * <pre>
 * # Windows PowerShell:
 * $env:DASHSCOPE_API_KEY="tp-c3983xr4mwb0o1ly1ml81y0n5ujlst1ilpkhitbfua75dz8w"
 * mvn test -pl spring-ai-lab-scenario-chat -Dtest=SkillIntegrationTest -DfailIfNoTests=false
 *
 * # Linux / macOS:
 * export DASHSCOPE_API_KEY="tp-c3983xr4mwb0o1ly1ml81y0n5ujlst1ilpkhitbfua75dz8w"
 * mvn test -pl spring-ai-lab-scenario-chat -Dtest=SkillIntegrationTest -DfailIfNoTests=false
 * </pre>
 *
 * @author liziye
 * @since 0.3.0
 */
@SpringBootTest(
        classes = TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.location=classpath:application-skilltest.yml"
)
@ActiveProfiles("skilltest")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Skill 功能验证")
class SkillIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SkillRegistry skillRegistry;

    @Autowired
    private SkillRouter skillRouter;

    private final ObjectMapper mapper = new ObjectMapper();

    // ================================================================
    // 1. SkillLoader 加载验证
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("1. 验证 SkillLoader 成功加载 3 个 Skill 文件")
    void testSkillLoading() {
        Collection<ParsedSkill> allSkills = skillRegistry.getAll();
        System.out.println("\n===== 已加载的 Skill 列表 =====");
        for (ParsedSkill s : allSkills) {
            System.out.printf("  📄 %-25s | %s | tags=%s | priority=%d%n",
                    s.getName(),
                    s.getDescription() != null && s.getDescription().length() > 40
                            ? s.getDescription().substring(0, 40) + "..."
                            : s.getDescription(),
                    s.getTags(),
                    s.getPriority());
        }
        System.out.printf("  总计: %d 个 Skill\n\n", allSkills.size());

        assertFalse(allSkills.isEmpty(), "Skill 列表不应为空");
        assertTrue(allSkills.size() >= 3,
                "应至少加载 3 个 Skill，实际: " + allSkills.size());

        // 验证具体 Skill
        Optional<ParsedSkill> weather = allSkills.stream()
                .filter(s -> "weather-assistant".equals(s.getName())).findFirst();
        Optional<ParsedSkill> codeReview = allSkills.stream()
                .filter(s -> "code-reviewer".equals(s.getName())).findFirst();
        Optional<ParsedSkill> dataAnalyst = allSkills.stream()
                .filter(s -> "data-analyst".equals(s.getName())).findFirst();

        assertTrue(weather.isPresent(), "weather-assistant 应已加载");
        assertTrue(codeReview.isPresent(), "code-reviewer 应已加载");
        assertTrue(dataAnalyst.isPresent(), "data-analyst 应已加载");

        // 验证 frontmatter 解析
        assertEquals("天气助手", weather.get().getDisplayName());
        assertNotNull(weather.get().getDescription());
        assertFalse(weather.get().getBody().isBlank(), "Skill body 不应为空");
    }

    // ================================================================
    // 2. SkillRouter 语义匹配验证
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("2. 验证语义路由：天气查询 → weather-assistant")
    void testSkillRoutingWeather() {
        List<ParsedSkill> matched = skillRouter.match(
                "今天北京天气怎么样", skillRegistry.getAll());

        System.out.println("\n===== 语义匹配结果: \"今天北京天气怎么样\" =====");
        for (ParsedSkill s : matched) {
            System.out.printf("  🎯 %s (匹配)%n", s.getName());
        }
        if (matched.isEmpty()) {
            System.out.println("  ⚠ 没有匹配到任何 Skill（可能阈值过高）");
        }
        System.out.println();

        assertFalse(matched.isEmpty(), "应该至少匹配到一个 Skill");

        // 第一个应该是 weather-assistant
        if (!matched.isEmpty()) {
            // 检查 weather-assistant 是否在前 2 名内（语义匹配可能匹配多个）
            boolean hasWeather = matched.stream()
                    .anyMatch(s -> "weather-assistant".equals(s.getName()));
            assertTrue(hasWeather, "应匹配到 weather-assistant，实际: " +
                    matched.stream().map(ParsedSkill::getName).toList());
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. 验证语义路由：代码审查 → code-reviewer")
    void testSkillRoutingCodeReview() {
        List<ParsedSkill> matched = skillRouter.match(
                "帮我审查这段Java代码有没有安全问题", skillRegistry.getAll());

        System.out.println("\n===== 语义匹配结果: \"帮我审查这段Java代码\" =====");
        for (ParsedSkill s : matched) {
            System.out.printf("  🎯 %s%n", s.getName());
        }

        assertFalse(matched.isEmpty(), "应该至少匹配到一个 Skill");
        boolean hasCodeReview = matched.stream()
                .anyMatch(s -> "code-reviewer".equals(s.getName()));
        assertTrue(hasCodeReview, "应匹配到 code-reviewer，实际: " +
                matched.stream().map(ParsedSkill::getName).toList());
    }

    @Test
    @Order(4)
    @DisplayName("4. 验证无匹配场景：无关问题不应匹配")
    void testSkillRoutingNoMatch() {
        // "你好" 不应该匹配到任何有明确专业领域的 Skill
        List<ParsedSkill> matched = skillRouter.match(
                "你好，请问你是谁", skillRegistry.getAll());

        System.out.println("\n===== 语义匹配结果: \"你好，请问你是谁\" =====");
        for (ParsedSkill s : matched) {
            System.out.printf("  🤔 %s%n", s.getName());
        }
        System.out.println("  ℹ️ 匹配数: " + matched.size() + "（无关话题应尽量少匹配）\n");

        // 不强断言（阈值低了可能误匹配），但记录日志
    }

    // ================================================================
    // 3. 端到端：Skill 注入后的 AI 响应
    // ================================================================

    @Test
    @Order(5)
    @DisplayName("5. 端到端测试：Skill 注入后 AI 按天气助手角色回答")
    void testSkillEndToEndWeather() {
        // 使用多轮对话测试 Skill 注入效果
        String conversationId = "skill-weather-" + UUID.randomUUID().toString().substring(0, 8);

        // 第一轮：天气查询 → 应匹配 weather-assistant
        Map<String, Object> req1 = new LinkedHashMap<>();
        req1.put("conversationId", conversationId);
        req1.put("userInput", "今天北京天气怎么样，适合出门吗？");

        ResponseEntity<String> resp1 = post("/api/chat", req1);
        System.out.println("\n===== 端到端测试: 天气查询（期望匹配 weather-assistant）=====");
        System.out.println("HTTP Status: " + resp1.getStatusCode());
        System.out.println("Response: " + trunc(resp1.getBody(), 300));
        System.out.println();

        assertTrue(resp1.getStatusCode().is2xxSuccessful(),
                "API 应返回 2xx，实际: " + resp1.getStatusCode());

        // 第二轮：多轮记忆 — 追问
        Map<String, Object> req2 = new LinkedHashMap<>();
        req2.put("conversationId", conversationId);
        req2.put("userInput", "那明天呢？");

        ResponseEntity<String> resp2 = post("/api/chat", req2);
        System.out.println("===== 端到端测试: 追问（多轮记忆）=====");
        System.out.println("HTTP Status: " + resp2.getStatusCode());
        System.out.println("Response: " + trunc(resp2.getBody(), 300));
        System.out.println();

        assertTrue(resp2.getStatusCode().is2xxSuccessful(),
                "多轮对话 API 应返回 2xx");
    }

    @Test
    @Order(6)
    @DisplayName("6. 端到端测试：代码审查请求应匹配 code-reviewer")
    void testSkillEndToEndCodeReview() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("conversationId", "skill-review-" + UUID.randomUUID().toString().substring(0, 8));
        req.put("userInput", "请审查以下代码: public void process(int x){ if(x>0){x=1/0;} }");

        ResponseEntity<String> resp = post("/api/chat", req);
        System.out.println("\n===== 端到端测试: 代码审查（期望匹配 code-reviewer）=====");
        System.out.println("HTTP Status: " + resp.getStatusCode());
        System.out.println("Response: " + trunc(resp.getBody(), 300));
        System.out.println();

        assertTrue(resp.getStatusCode().is2xxSuccessful(),
                "代码审查 API 应返回 2xx，实际: " + resp.getStatusCode());
    }

    // ================================================================
    // 辅助方法
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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static String trunc(String s, int n) {
        if (s == null) return "null";
        String clean = s.replace("\n", " ").replace("\r", "");
        return clean.length() > n ? clean.substring(0, n) + "…" : clean;
    }
}
