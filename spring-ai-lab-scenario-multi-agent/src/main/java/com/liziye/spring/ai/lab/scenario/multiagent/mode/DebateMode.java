package com.liziye.spring.ai.lab.scenario.multiagent.mode;

import com.liziye.spring.ai.lab.scenario.multiagent.model.MultiAgentContext;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 辩论模式：多个 Agent 互相辩论，通过多轮对话达成共识或找出分歧。
 *
 * <p>适用场景：方案选型讨论、风险评估、策略制定。
 *
 * @author liziye
 * @since 1.0.0
 */
public class DebateMode {

    /**
     * 辩论执行：Agent 轮流发言，互相质询和回应。
     *
     * @param chatClient  {@link ChatClient} 实例
     * @param userInput   用户输入
     * @param context     多 Agent 上下文
     * @param agentRunner Agent 执行函数
     * @return 辩论结果
     */
    public static String execute(ChatClient chatClient,
                                  String userInput,
                                  MultiAgentContext context,
                                  SequentialMode.AgentRunner agentRunner) {

        int debateRounds = context.getMaxDebateRounds() > 0 ? context.getMaxDebateRounds() : 2;
        StringBuilder transcript = new StringBuilder();

        transcript.append("# 多 Agent 辩论记录\n\n");
        transcript.append("## 辩论主题\n").append(userInput).append("\n\n");

        // 第一轮：各方陈述立场
        transcript.append("## 第一轮：立场陈述\n\n");
        String previousStatements = "";
        for (MultiAgentContext.AgentRole role : context.getAgentRoles()) {
            String taskInput = "你正在参与一场关于以下议题的多方辩论。\n\n"
                    + "### 辩论主题\n" + userInput + "\n\n"
                    + "### 已发言的 Agent\n" + (previousStatements.isEmpty() ? "（你是第一个发言的）" : previousStatements) + "\n\n"
                    + "### 你的角色\n" + role.getName() + ": " + role.getDescription() + "\n\n"
                    + "请作为【" + role.getName() + "】陈述你的立场和论据。用中文回答。";

            String statement = agentRunner.run(chatClient, role, taskInput);
            transcript.append("### ").append(role.getName()).append("\n").append(statement).append("\n\n");
            previousStatements += "**" + role.getName() + "**: " + truncate(statement, 300) + "\n\n";
        }

        // 后续回合：质询与回应
        for (int round = 1; round < debateRounds; round++) {
            transcript.append("## 第").append(round + 1).append("轮：质询与回应\n\n");

            for (MultiAgentContext.AgentRole role : context.getAgentRoles()) {
                String taskInput = "这是辩论的第 " + (round + 1) + " 轮。\n\n"
                        + "### 辩论主题\n" + userInput + "\n\n"
                        + "### 完整辩论记录（前序）\n" + transcript + "\n\n"
                        + "你作为【" + role.getName() + "】，请对其他 Agent 的观点进行质询或回应，"
                        + "同时补充你新的论据。用中文回答。";

                String response = agentRunner.run(chatClient, role, taskInput);
                transcript.append("### ").append(role.getName()).append("\n").append(response).append("\n\n");
            }
        }

        // 最终总结
        transcript.append("## 辩论总结\n\n");
        String summaryPrompt = "你是一位公正的辩论主席。请基于以下辩论记录，"
                + "总结各方观点的共识和分歧，并给出客观的综合判断。\n\n"
                + "### 原始议题\n" + userInput + "\n\n"
                + "### 辩论记录\n" + transcript + "\n\n"
                + "请用中文总结。";

        try {
            String summary = chatClient.prompt().user(summaryPrompt).call().content();
            transcript.append(summary);
        } catch (Exception e) {
            transcript.append("（总结生成失败：").append(e.getMessage()).append("）");
        }

        return transcript.toString();
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
