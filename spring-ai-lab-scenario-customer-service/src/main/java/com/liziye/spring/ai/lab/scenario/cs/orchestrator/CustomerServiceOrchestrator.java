package com.liziye.spring.ai.lab.scenario.cs.orchestrator;

import com.liziye.spring.ai.lab.core.memory.ConversationMemory;
import com.liziye.spring.ai.lab.core.model.AgentResponse;
import com.liziye.spring.ai.lab.core.model.Message;
import com.liziye.spring.ai.lab.core.observation.LatencyMetrics;
import com.liziye.spring.ai.lab.core.observation.TokenMetrics;
import com.liziye.spring.ai.lab.core.routing.DefaultModelProviderManager;
import com.liziye.spring.ai.lab.scenario.cs.CustomerServiceProperties;
import com.liziye.spring.ai.lab.scenario.cs.intent.IntentClassifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 智能客服编排器。
 *
 * <p>负责意图识别、情感安抚、智能路由和对话管理。
 *
 * <p>处理流程：
 * <ol>
 *   <li>接收用户输入</li>
 *   <li>意图分类（投诉/咨询/反馈/闲聊/售后）</li>
 *   <li>加载历史对话上下文</li>
 *   <li>构建场景化 Prompt（含情感安抚信息）</li>
 *   <li>调用 LLM 生成回复</li>
 *   <li>保存对话记忆 + 记录指标</li>
 * </ol>
 *
 * <p>使用 {@link IntentClassifier} 进行基于关键词的意图识别，
 * 通过 {@link com.liziye.spring.ai.lab.core.memory.ConversationMemory} 管理多轮对话。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class CustomerServiceOrchestrator {

    private final DefaultModelProviderManager modelManager;
    private final ConversationMemory memory;
    private final IntentClassifier intentClassifier;
    private final TokenMetrics tokenMetrics;
    private final LatencyMetrics latencyMetrics;
    private final CustomerServiceProperties properties;

    public CustomerServiceOrchestrator(
            DefaultModelProviderManager modelManager,
            ConversationMemory memory,
            IntentClassifier intentClassifier,
            TokenMetrics tokenMetrics,
            LatencyMetrics latencyMetrics,
            CustomerServiceProperties properties) {
        this.modelManager = modelManager;
        this.memory = memory;
        this.intentClassifier = intentClassifier;
        this.tokenMetrics = tokenMetrics;
        this.latencyMetrics = latencyMetrics;
        this.properties = properties;
    }

    /**
     * 同步执行智能客服对话。
     *
     * <p>完整流程：意图分类 → 加载历史 → 构建场景化 Prompt → 调用 LLM → 保存记忆和指标。
     *
     * @param conversationId 会话 ID
     * @param userInput      用户输入文本
     * @return Agent 响应，包含回复内容、意图标签、置信度、Token 估算、延迟等
     */
    public AgentResponse execute(String conversationId, String userInput) {
        long startTime = System.currentTimeMillis();

        // 1. 意图分类
        IntentClassifier.IntentResult intent = intentClassifier.classify(userInput);

        // 2. 加载历史
        List<Message> history = memory.getHistory(conversationId, properties.getMaxTurns() * 2);

        // 3. 构建 Prompt
        String systemPrompt = buildScopedPrompt(intent);

        // 4. 保存用户消息
        memory.addMessage(conversationId, new Message("user", userInput, LocalDateTime.now()));

        // 5. 调用 LLM
        ChatModel chatModel = modelManager.getDefaultModel();
        String modelName = "default";

        try {
            // 构建消息列表
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            // 添加历史消息（最近 N 轮）
            int historyLimit = Math.min(history.size(), properties.getMaxTurns() * 2);
            for (int i = Math.max(0, history.size() - historyLimit); i < history.size(); i++) {
                Message msg = history.get(i);
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }

            // 当前用户消息
            messages.add(new UserMessage(userInput));

            Prompt prompt = new Prompt(messages);
            var response = chatModel.call(prompt);
            String replyContent = response.getResult().getOutput().getText();

            long latency = System.currentTimeMillis() - startTime;

            // 估算 Token（简单按字符数估算）
            long estimatedTokens = (systemPrompt.length() + userInput.length() + replyContent.length()) / 4;

            // 6. 保存 AI 回复
            memory.addMessage(conversationId, new Message("assistant", replyContent, LocalDateTime.now()));

            // 7. 记录指标
            tokenMetrics.recordUsage(modelName, estimatedTokens);
            latencyMetrics.recordLatency("customer-service", latency);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("intent", intent.getIntent());
            metadata.put("confidence", String.format("%.2f", intent.getConfidence()));
            metadata.put("tokens", estimatedTokens);
            metadata.put("latencyMs", latency);
            metadata.put("model", modelName);
            metadata.put("empathyEnabled", properties.isEmpathyEnabled());

            log.info("[CS] conversation={} intent={} confidence={:.2f} tokens={} latency={}ms",
                    conversationId, intent.getIntent(), intent.getConfidence(), estimatedTokens, latency);

            return AgentResponse.builder()
                    .content(replyContent)
                    .conversationId(conversationId)
                    .metadata(metadata)
                    .fallback(false)
                    .build();

        } catch (Exception e) {
            log.error("[CS] conversation={} error={}", conversationId, e.getMessage(), e);
            long latency = System.currentTimeMillis() - startTime;
            latencyMetrics.recordLatency("customer-service-error", latency);

            return AgentResponse.builder()
                    .content("抱歉，系统暂时遇到一些问题，请您稍后再试。给您带来的不便敬请谅解。")
                    .conversationId(conversationId)
                    .metadata(Map.of("error", e.getMessage(), "latencyMs", latency))
                    .fallback(true)
                    .build();
        }
    }

    /**
     * 构建场景化 System Prompt（根据意图定制）。
     */
    private String buildScopedPrompt(IntentClassifier.IntentResult intent) {
        String basePrompt = properties.buildSystemPrompt();

        String intentGuidance = switch (intent.getIntent()) {
            case "投诉" -> """

                    ## 当前客户意图：投诉 ⚠️
                    - 这是高优先级情况，请立即道歉并使用安抚性语言
                    - 主动询问问题详情，记录关键信息
                    - 承诺会在 X 小时内跟进处理
                    - 可以适当提供补偿方案以安抚客户
                    """;
            case "咨询" -> """

                    ## 当前客户意图：咨询 ℹ️
                    - 给出准确、全面、易懂的答案
                    - 如果不确定细节，可以引导客户提供更多信息
                    - 提供相关的产品或服务链接（如有）
                    """;
            case "反馈" -> """

                    ## 当前客户意图：反馈 📝
                    - 真诚感谢客户的反馈
                    - 确认已经记录，会传达给相关团队
                    - 如有改进时间线可告知客户
                    """;
            case "售后" -> """

                    ## 当前客户意图：售后 🔧
                    - 确认产品型号、购买时间、问题现象
                    - 按售后流程引导客户
                    - 提供明确的下一步操作指引
                    """;
            default -> """

                    ## 当前客户意图：闲聊 💬
                    - 以轻松友好的方式回应
                    - 可以适当展示品牌个性
                    - 同时引导客户是否需要帮助
                    """;
        };

        return basePrompt + intentGuidance;
    }

    /**
     * 清除会话。
     *
     * @param conversationId 会话 ID
     */
    public void clearSession(String conversationId) {
        memory.clear(conversationId);
        log.info("[CS] Session cleared: conversation={}", conversationId);
    }

    /**
     * 获取会话消息数。
     *
     * @param conversationId 会话 ID
     * @return 会话中的消息数量
     */
    public int getMessageCount(String conversationId) {
        return memory.getMessageCount(conversationId);
    }
}
