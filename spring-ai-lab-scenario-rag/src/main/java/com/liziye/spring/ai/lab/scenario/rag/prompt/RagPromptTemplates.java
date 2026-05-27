package com.liziye.spring.ai.lab.scenario.rag.prompt;

/**
 * RAG 提示词模板。
 *
 * <p>提供中英文系统提示以及增强查询的模板构建方法。
 * 所有常量和方法均为静态，不可实例化。
 *
 * @author liziye
 * @since 1.0.0
 */
public final class RagPromptTemplates {

    private RagPromptTemplates() {}

    /** 中文系统提示 */
    public static final String SYSTEM_PROMPT_ZH = """
            你是一个基于知识库的问答助手。请基于提供的参考资料回答用户问题。
                        
            回答规则：
            1. 如果参考资料中包含答案，请引用资料内容并标注来源
            2. 如果参考资料中没有答案，请如实告知用户"抱歉，当前知识库中没有相关信息"
            3. 保持回答清晰、准确、有条理
            4. 当引用参考资料时，请使用 [参考1]、[参考2] 等编号标注
            """;

    /** 英文系统提示 */
    public static final String SYSTEM_PROMPT_EN = """
            You are a knowledge-based Q&A assistant. Answer user questions based on the provided reference materials.
                        
            Rules:
            1. If the answer is in the references, cite the source
            2. If the answer is not in the references, tell the user honestly
            3. Keep responses clear, accurate, and well-organized
            4. When citing references, use [Ref1], [Ref2] notation
            """;

    /**
     * 构建增强提问模板。
     *
     * <p>将参考资料和用户问题组合为增强查询的 Prompt 文本。
     *
     * @param userQuery  用户问题
     * @param documents  格式化的参考资料文本
     * @return 包含参考资料和用户问题的增强提问模板
     */
    public static String buildAugmentedQuery(String userQuery, String documents) {
        return String.format("""
                === Reference Materials ===
                %s
                                
                === User Question ===
                %s
                                
                Please answer based on the reference materials above. Cite sources using reference numbers.
                """, documents, userQuery);
    }
}
