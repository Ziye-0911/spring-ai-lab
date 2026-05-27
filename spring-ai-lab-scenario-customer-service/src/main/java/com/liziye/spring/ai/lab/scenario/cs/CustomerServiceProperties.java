package com.liziye.spring.ai.lab.scenario.cs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智能客服配置属性。
 *
 * <p>配置前缀：{@code spring.ai.lab.customer-service}。
 * 包含客服基础配置（名称、语言、最大对话轮次、情感安抚开关等）、
 * 知识库路径、意图识别参数，以及中英文双语 System Prompt 构建能力。
 *
 * <p>通过 {@link #buildSystemPrompt()} 方法按语言环境生成对应的客服系统提示。
 *
 * @author liziye
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.lab.customer-service")
public class CustomerServiceProperties {

    /** Prompt 语言：zh / en */
    private String locale = "zh";

    /** 最大对话轮次 */
    private int maxTurns = 20;

    /** 是否启用情感安抚 */
    private boolean empathyEnabled = true;

    /** 客服名称 */
    private String agentName = "小智";

    /** 知识库文档路径（可选，用于 RAG 增强） */
    private String knowledgeBasePath;

    /** 意图识别 */
    private Intent intent = new Intent();

    @Data
    public static class Intent {

        /** 意图识别模型温度 */
        private double temperature = 0.3;

        /** 意图分类阈值（低于此分数的归为"闲聊"） */
        private double confidenceThreshold = 0.6;

        /** 意图列表 */
        private String categories = "投诉,咨询,反馈,闲聊,售后";
    }

    /**
     * 构建客服 System Prompt。
     *
     * <p>根据 {@code locale}（zh/en）生成对应的客服系统提示，包含角色定位、
     * 行为准则、禁止行为和回复格式要求。若启用了情感安抚（{@code empathyEnabled}），
     * 会新增共情引导说明。
     *
     * @return 对应语言的客服 System Prompt 文本
     */
    public String buildSystemPrompt() {
        String name = agentName;
        if ("zh".equals(locale)) {
            return """
                    你是「%s」智能客服助手，请严格遵守以下规则：

                    ## 角色定位
                    - 你是专业的客户服务代表，代表公司服务客户
                    - 始终保持礼貌、耐心、专业的态度

                    ## 行为准则
                    1. 首先理解客户意图：投诉、咨询、反馈、闲聊、售后
                    2. 如果是投诉：先真诚道歉，记录问题，承诺跟进处理
                    3. 如果是咨询：给出准确、完整的信息
                    4. 如果是反馈：感谢客户的意见，记录并传达
                    5. 永远不要对客户说"我不知道"，而是说"我帮您查询一下"
                    %s

                    ## 禁止行为
                    - 禁止泄露任何内部信息
                    - 禁止做出无法兑现的承诺
                    - 禁止与客户争论或推卸责任
                    - 禁止使用不礼貌或不专业的语言

                    ## 回复格式
                    - 使用友好、亲切的语气
                    - 适当使用表情符号增强亲和力
                    - 复杂问题分点列出，便于阅读
                    - 结束时确认客户是否还有其他需求
                    """.formatted(name, empathyEnabled ? """
                    - 在客户表达不满时，先进行情感安抚再解决问题
                    - 使用共情语言让客户感到被理解和重视
                    """ : "");
        } else {
            return """
                    You are the "%s" intelligent customer service assistant.
                    Please strictly follow these rules:

                    ## Role
                    - Professional customer service representative
                    - Always polite, patient, and professional

                    ## Guidelines
                    1. Understand customer intent: complaint, inquiry, feedback, chat, after-sales
                    2. Complaint: Apologize first, record issue, promise follow-up
                    3. Inquiry: Provide accurate and complete information
                    4. Feedback: Thank the customer, record and relay
                    5. Never say "I don't know", instead say "Let me check for you"
                    %s

                    ## Prohibited
                    - No disclosure of internal information
                    - No promises that cannot be fulfilled
                    - No arguing or blame-shifting
                    - No impolite or unprofessional language

                    ## Reply Format
                    - Friendly and warm tone
                    - Use bullet points for complex issues
                    - Confirm customer needs at the end
                    """.formatted(name, empathyEnabled ? """
                    - When customer expresses dissatisfaction, first empathize emotionally
                    - Use empathetic language to make the customer feel understood
                    """ : "");
        }
    }
}
