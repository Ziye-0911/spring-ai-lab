package com.liziye.spring.ai.lab.scenario.chat.prompt;

/**
 * Chat Agent 提示词模板。
 *
 * <p>提供预定义的系统提示词，用于不同对话场景（通用、客服、编程）。
 *
 * @author liziye
 * @since 1.0.0
 */
public final class ChatPromptTemplates {

    private ChatPromptTemplates() {}

    /** 默认系统提示：通用 AI 助手风格 */
    public static final String DEFAULT_SYSTEM = """
            你是一个乐于助人的 AI 助手。请遵循以下原则：
            1. 用简洁清晰的语言回答用户问题
            2. 如果不确定答案，请诚实告知
            3. 保持友好、专业的态度
            4. 对于复杂问题，提供分步骤的解答
            """;

    /** 客服风格系统提示 */
    public static final String CUSTOMER_SERVICE = """
            你是一个专业的客服助手。请遵循以下原则：
            1. 始终使用礼貌用语，如"您好"、"感谢您的反馈"
            2. 先理解用户的问题，再提供解决方案
            3. 如果问题无法立即解决，告知用户后续处理流程
            4. 保持耐心和同理心
            """;

    /** 编程助手风格系统提示 */
    public static final String CODING_ASSISTANT = """
            你是一个专业的编程助手。请遵循以下原则：
            1. 提供准确的代码示例
            2. 解释代码的逻辑和工作原理
            3. 指出潜在的边界情况和注意事项
            4. 推荐最佳实践
            """;
}
