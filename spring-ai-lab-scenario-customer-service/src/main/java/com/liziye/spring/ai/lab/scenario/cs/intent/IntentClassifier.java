package com.liziye.spring.ai.lab.scenario.cs.intent;

import com.liziye.spring.ai.lab.scenario.cs.CustomerServiceProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 意图分类器。
 *
 * <p>基于规则 + 关键词匹配的轻量意图识别，支持 5 类意图：
 * 投诉、咨询、反馈、闲聊、售后。
 * 可通过 configuration properties 扩展关键词库。
 *
 * <p>核心方法 {@link #classify(String)} 根据用户输入匹配关键词，
 * 计算加权分数，返回最佳匹配的意图标签和置信度。
 * 对强烈情感表达会自动提高投诉意图的权重。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class IntentClassifier {

    private final CustomerServiceProperties properties;

    /** 意图 → 关键词列表 */
    private final Map<String, List<String>> keywordMap = new LinkedHashMap<>();

    /** 意图 → 匹配权重 */
    private final Map<String, Integer> weights = new LinkedHashMap<>();

    public IntentClassifier(CustomerServiceProperties properties) {
        this.properties = properties;
        initKeywords();
    }

    private void initKeywords() {
        // 投诉
        keywordMap.put("投诉", List.of(
                "投诉", "不满意", "差评", "太差了", "坑人", "退款",
                "投诉你", "我要举报", "什么态度", "气死我了", "骗人",
                "垃圾", "太过分了", "没人管吗", "忍不了"
        ));
        weights.put("投诉", 10);

        // 咨询
        keywordMap.put("咨询", List.of(
                "怎么", "如何", "请问", "多少钱", "价格", "什么时候",
                "在哪里", "可以吗", "能不能", "支持", "有没有",
                "怎么用", "是什么", "介绍一下", "了解", "咨询"
        ));
        weights.put("咨询", 8);

        // 反馈
        keywordMap.put("反馈", List.of(
                "建议", "意见", "反馈", "希望能", "推荐", "想法",
                "要是能", "能不能加", "功能建议", "体验建议"
        ));
        weights.put("反馈", 7);

        // 售后
        keywordMap.put("售后", List.of(
                "退换", "保修", "维修", "换货", "退货", "坏了",
                "质量问题", "不工作", "故障", "修理", "售后"
        ));
        weights.put("售后", 9);

        // 闲聊（兜底）
        keywordMap.put("闲聊", List.of(
                "你好", "谢谢", "再见", "天气", "今天", "哈哈"
        ));
        weights.put("闲聊", 1);
    }

    /**
     * 识别用户输入的意图。
     *
     * @param userInput 用户输入
     * @return 分类结果（意图标签 + 置信度）
     */
    public IntentResult classify(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return new IntentResult("闲聊", 0.0, "empty input");
        }

        String lowerInput = userInput.toLowerCase();

        // 情感强烈 → 提高投诉权重
        boolean hasStrongEmotion = containsStrongEmotion(lowerInput);

        IntentResult best = new IntentResult("闲聊", 0.0, "default");

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            int baseWeight = weights.getOrDefault(category, 1);

            int matchCount = 0;
            for (String keyword : keywords) {
                if (lowerInput.contains(keyword)) {
                    matchCount++;
                }
            }

            if (matchCount > 0) {
                double score = Math.min(1.0, (double) matchCount / keywords.size())
                        * baseWeight / 10.0;
                // 强烈情感 → 投诉额外加分
                if ("投诉".equals(category) && hasStrongEmotion) {
                    score = Math.min(1.0, score + 0.3);
                }
                if (score > best.getConfidence()) {
                    best = new IntentResult(category, score,
                            "matched " + matchCount + " keywords in " + category);
                }
            }
        }

        log.debug("[INTENT] input=\"{}\" → intent={} confidence={:.2f}",
                truncate(userInput, 50), best.getIntent(), best.getConfidence());

        return best;
    }

    private boolean containsStrongEmotion(String text) {
        return Pattern.compile("[!！？?]{2,}|气死|太过分|坑人|骗人|垃圾|投诉|差评")
                .matcher(text)
                .find();
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    // ===== Result =====

    public static class IntentResult {
        private final String intent;
        private final double confidence;
        private final String reason;

        public IntentResult(String intent, double confidence, String reason) {
            this.intent = intent;
            this.confidence = confidence;
            this.reason = reason;
        }

        public String getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }

        /** 是否为低置信度（低于阈值视为闲聊）。 */
        public boolean isLowConfidence(double threshold) {
            return confidence < threshold;
        }
    }
}
