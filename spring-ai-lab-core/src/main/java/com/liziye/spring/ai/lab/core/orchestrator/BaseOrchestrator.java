package com.liziye.spring.ai.lab.core.orchestrator;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentContext;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.Message;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.ModelProviderManager;
import com.liziye.spring.ai.lab.core.skill.ParsedSkill;
import com.liziye.spring.ai.lab.core.skill.SkillRegistry;
import com.liziye.spring.ai.lab.core.skill.SkillRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * 通用编排基类 — 封装所有场景共用的编排逻辑。
 *
 * <p>职责：
 * <ol>
 *   <li>会话记忆管理（加载历史、追加新消息）</li>
 *   <li>Token 统计与延迟监控</li>
 *   <li>日志记录（每次调用的输入/输出/耗时）</li>
 *   <li>模型调用（通过 {@link com.liziye.spring.ai.lab.core.routing.ModelRouter} 选择模型）</li>
 *   <li>异常处理与降级</li>
 * </ol>
 *
 * <p>子类只需实现场景差异化逻辑（如 RAG 的检索 + Prompt 组装）。
 *
 * <p>模板方法模式：
 * <pre>{@code
 *   execute() 定义了编排骨架
 *     → preProcess()    子类可覆盖，预处理上下文
 *     → doExecute()     抽象方法，子类实现核心逻辑
 *     → postProcess()   子类可覆盖，后处理结果
 *     → updateMemory()  自动保存对话历史
 *     → recordMetrics() 自动记录指标
 * }</pre>
 *
 * @param <T> 场景 Context 类型
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public abstract class BaseOrchestrator<T extends AgentContext> implements AgentOrchestrator<T> {

    protected final ModelProviderManager modelManager;
    protected final ConversationMemory memory;
    protected final List<Advisor> advisors;
    protected final TokenMetrics tokenMetrics;
    protected final LatencyMetrics latencyMetrics;
    protected final SkillRegistry skillRegistry;
    protected final SkillRouter skillRouter;

    /**
     * 构造编排器（不含 Skill 支持，向后兼容）。
     */
    protected BaseOrchestrator(ModelProviderManager modelManager,
                               ConversationMemory memory,
                               List<Advisor> advisors,
                               TokenMetrics tokenMetrics,
                               LatencyMetrics latencyMetrics) {
        this(modelManager, memory, advisors, tokenMetrics, latencyMetrics, null, null);
    }

    /**
     * 构造编排器（含 Skill 支持）。
     *
     * @param skillRegistry Skill 注册中心，可为 null
     * @param skillRouter   Skill 路由器，可为 null
     */
    protected BaseOrchestrator(ModelProviderManager modelManager,
                               ConversationMemory memory,
                               List<Advisor> advisors,
                               TokenMetrics tokenMetrics,
                               LatencyMetrics latencyMetrics,
                               SkillRegistry skillRegistry,
                               SkillRouter skillRouter) {
        this.modelManager = modelManager;
        this.memory = memory;
        this.advisors = advisors != null ? advisors : Collections.emptyList();
        this.tokenMetrics = tokenMetrics;
        this.latencyMetrics = latencyMetrics;
        this.skillRegistry = skillRegistry;
        this.skillRouter = skillRouter;
    }

    @Override
    public AgentResponse execute(String conversationId, String userInput, T context) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 预处理
            preProcess(conversationId, userInput, context);

            // 2. 获取 ChatClient
            ChatClient chatClient = resolveChatClient(context);

            // 3. 构建带记忆、Advisor 和 Skill 上下文的 ChatClient
            ChatClient configuredClient = buildConfiguredClient(chatClient, conversationId, userInput, context);

            // 4. 执行核心逻辑（子类实现）
            AgentResponse response = doExecute(configuredClient, userInput, context);

            // 5. 后处理
            response = postProcess(response, context);

            // 6. 更新对话记忆
            updateMemory(conversationId, userInput, response);

            // 7. 记录指标
            long elapsed = System.currentTimeMillis() - startTime;
            recordMetrics(conversationId, userInput, response, elapsed);

            response.setConversationId(conversationId);
            return response;

        } catch (Exception e) {
            log.error("[ORCHESTRATOR] conversation={} error={}", conversationId, e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            recordErrorMetrics(conversationId, e, elapsed);
            throw e; // 重新抛出，让 GlobalExceptionHandler 统一处理
        }
    }

    /**
     * 子类实现场景核心逻辑。
     *
     * @param chatClient 已配置好的 ChatClient（含记忆、Advisors）
     * @param userInput  用户输入
     * @param context    场景上下文
     * @return Agent 响应
     */
    protected abstract AgentResponse doExecute(ChatClient chatClient,
                                                String userInput,
                                                T context);

    /**
     * 预处理（子类可覆盖）。
     */
    protected void preProcess(String conversationId, String userInput, T context) {
        log.info("[ORCHESTRATOR] conversation={} user_input_preview={}",
                conversationId, truncate(userInput, 100));
    }

    /**
     * 后处理（子类可覆盖）。
     */
    protected AgentResponse postProcess(AgentResponse response, T context) {
        return response;
    }

    /**
     * 更新对话记忆。
     */
    protected void updateMemory(String conversationId, String userInput, AgentResponse response) {
        try {
            memory.addMessage(conversationId, Message.user(userInput));
            memory.addMessage(conversationId, Message.assistant(response.getContent()));
        } catch (Exception e) {
            log.warn("[ORCHESTRATOR] Failed to update memory for conversation={}", conversationId, e);
        }
    }

    /**
     * 记录成功指标。
     */
    protected void recordMetrics(String conversationId, String userInput,
                                  AgentResponse response, long elapsedMs) {
        if (tokenMetrics != null) {
            tokenMetrics.recordUsage((String) response.getMetadata().getOrDefault("model", "unknown"),
                    getTokenCount(response));
        }
        if (latencyMetrics != null) {
            latencyMetrics.recordLatency(getOrchestratorName(), elapsedMs);
        }
    }

    /**
     * 记录错误指标。
     */
    protected void recordErrorMetrics(String conversationId, Exception e, long elapsedMs) {
        if (latencyMetrics != null) {
            latencyMetrics.recordLatency(getOrchestratorName(), elapsedMs);
        }
    }

    /**
     * 解析 ChatClient（根据 Context 中的模型提供商选择）。
     */
    protected ChatClient resolveChatClient(T context) {
        if (context.getModelProvider() != null && modelManager != null) {
            return modelManager.getChatClient(context.getModelProvider());
        }
        if (modelManager != null) {
            return modelManager.getDefaultChatClient();
        }
        throw new IllegalStateException("No ChatClient available");
    }

    /**
     * 构建配置了记忆、Advisor 和 Skill 上下文的 ChatClient。
     *
     * <p>当 Skill 系统启用时，根据用户输入自动匹配最合适的 Skill，
     * 并将其系统提词存入 Context 的 metadata 中（key = "skill_system_prompt"），
     * 供子类的 {@link #doExecute} 使用。
     *
     * @param chatClient     原始 ChatClient
     * @param conversationId 会话 ID
     * @param userInput      用户输入
     * @param context        场景上下文
     * @return 配置后的 ChatClient
     */
    protected ChatClient buildConfiguredClient(ChatClient chatClient, String conversationId,
                                                String userInput, T context) {
        // 注入 Skill 上下文
        applySkillContext(userInput, context);

        return chatClient;
    }

    /**
     * 构建配置了记忆和 Advisor 的 ChatClient（向后兼容，不含 Skill）。
     *
     * @deprecated 请使用 {@link #buildConfiguredClient(ChatClient, String, String, AgentContext)}
     */
    @Deprecated
    protected ChatClient buildConfiguredClient(ChatClient chatClient, String conversationId) {
        return chatClient;
    }

    /**
     * 执行 Skill 匹配并将匹配到的 Skill 系统提词存入上下文。
     *
     * <p>子类的 {@link #doExecute} 可通过以下方式获取 Skill 增强后的系统提词：
     * <pre>{@code
     * String skillPrompt = getSkillSystemPrompt(context);
     * if (skillPrompt != null) {
     *     systemPrompt = systemPrompt + "\n\n" + skillPrompt;
     * }
     * }</pre>
     */
    protected void applySkillContext(String userInput, T context) {
        if (skillRegistry == null || skillRouter == null
                || userInput == null || userInput.isBlank()) {
            return;
        }

        try {
            List<ParsedSkill> matched = skillRouter.match(userInput, skillRegistry.getAll());
            if (matched.isEmpty()) return;

            String skillPrompt = buildSkillPrompt(matched);
            context.getMetadata().put("skill_system_prompt", skillPrompt);
            context.getMetadata().put("matched_skills", matched.stream()
                    .map(ParsedSkill::getName).toList());

            log.debug("[SKILL] Matched {} skills for conversation: {}",
                    matched.size(), matched.stream().map(ParsedSkill::getName).toList());
        } catch (Exception e) {
            log.warn("[SKILL] Failed to match skills: {}", e.getMessage());
        }
    }

    /**
     * 获取 Skill 增强后的系统提词（供子类在 doExecute 中使用）。
     *
     * @param context 场景上下文
     * @return Skill 系统提词，如果未启用或未匹配到则返回 null
     */
    protected String getSkillSystemPrompt(T context) {
        Object prompt = context.getMetadata().get("skill_system_prompt");
        return prompt instanceof String ? (String) prompt : null;
    }

    /**
     * 将匹配到的 Skill 列表构建为系统提词。
     */
    private String buildSkillPrompt(List<ParsedSkill> skills) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前激活的技能\n\n");

        for (int i = 0; i < skills.size(); i++) {
            ParsedSkill skill = skills.get(i);
            sb.append("### 技能").append(i + 1).append("：")
                    .append(skill.getDisplayName() != null
                            ? skill.getDisplayName() : skill.getName()).append("\n\n");

            if (skill.getDescription() != null && !skill.getDescription().isBlank()) {
                sb.append("**描述**：").append(skill.getDescription()).append("\n\n");
            }

            sb.append("**指令**：\n").append(skill.getBody()).append("\n");

            if (i < skills.size() - 1) {
                sb.append("\n---\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 从响应中提取 Token 数量。
     */
    protected long getTokenCount(AgentResponse response) {
        if (response.getMetadata() != null && response.getMetadata().containsKey("tokens")) {
            Object tokens = response.getMetadata().get("tokens");
            if (tokens instanceof Number) {
                return ((Number) tokens).longValue();
            }
        }
        return 0;
    }

    /**
     * 获取编排器名称（用于指标）。
     */
    protected String getOrchestratorName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Flux<AgentResponse> executeStream(String conversationId, String userInput, T context) {
        return Flux.just(execute(conversationId, userInput, context));
    }

    @Override
    public List<String> getAvailableTools() {
        return Collections.emptyList();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
