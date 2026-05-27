package com.liziye.spring.ai.lab.scenario.multiagent.mode;

import com.liziye.spring.ai.lab.scenario.multiagent.model.MultiAgentContext;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 路由模式：根据用户输入的意图，自动选择一个最合适的 Agent 来处理。
 *
 * <p>适用场景：用户意图识别 → 路由到对应的专家 Agent，
 *          例如：安全问题 → 安全审查 Agent，性能问题 → 性能优化 Agent。
 *
 * @author liziye
 * @since 1.0.0
 */
public class RouterMode {

    /**
     * 路由执行：先分类用户意图，再委派给对应 Agent。
     *
     * @param chatClient  {@link ChatClient} 实例
     * @param userInput   用户输入
     * @param context     多 Agent 上下文
     * @param agentRunner Agent 执行函数
     * @return 执行结果
     */
    public static String execute(ChatClient chatClient,
                                  String userInput,
                                  MultiAgentContext context,
                                  SequentialMode.AgentRunner agentRunner) {
        // Step 1: 意图分类
        String selectedAgent = classifyIntent(chatClient, userInput, context);

        // Step 2: 找到对应 Agent 并执行
        MultiAgentContext.AgentRole matchedRole = context.getAgentRoles().stream()
                .filter(r -> r.getName().contains(selectedAgent) || selectedAgent.contains(r.getName()))
                .findFirst()
                .orElse(context.getAgentRoles().get(0));

        String result = agentRunner.run(chatClient, matchedRole, buildRouterInput(userInput, matchedRole));

        return "## 路由决策\n"
                + "根据用户意图分析，任务路由到: **" + matchedRole.getName() + "**\n\n"
                + "---\n\n"
                + "## " + matchedRole.getName() + " 的分析\n"
                + result;
    }

    /**
     * 分类用户意图，选择最合适的 Agent。
     */
    private static String classifyIntent(ChatClient chatClient,
                                          String userInput,
                                          MultiAgentContext context) {
        StringBuilder roleList = new StringBuilder();
        for (int i = 0; i < context.getAgentRoles().size(); i++) {
            MultiAgentContext.AgentRole role = context.getAgentRoles().get(i);
            roleList.append(i + 1).append(". ")
                    .append(role.getName()).append(": ")
                    .append(role.getDescription()).append("\n");
        }

        String prompt = "你是一个任务路由器。请分析以下用户需求，并从可用 Agent 中选择最合适的一个来处理。\n\n"
                + "### 可用 Agent\n" + roleList + "\n"
                + "### 用户需求\n" + userInput + "\n\n"
                + "请只回复最合适 Agent 的名称（精确匹配上面列表中的名称），不要包含其他内容。\n"
                + "格式：Agent名称";

        try {
            return chatClient.prompt().user(prompt).call().content().trim();
        } catch (Exception e) {
            // 默认选择第一个
            return context.getAgentRoles().get(0).getName();
        }
    }

    private static String buildRouterInput(String userInput, MultiAgentContext.AgentRole role) {
        return "## 任务\n\n"
                + "请从【" + role.getName() + "】的视角分析以下用户需求：\n\n"
                + userInput + "\n\n"
                + "你的角色: " + role.getName() + " - " + role.getDescription() + "\n"
                + "请用中文给出专业分析。";
    }
}
