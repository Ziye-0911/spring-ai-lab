package com.liziye.spring.ai.lab.core.skill;

import com.liziye.spring.ai.lab.core.config.SkillProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于语义相似度的 Skill 路由器。
 *
 * <p>使用 Bigram Jaccard 相似度计算用户输入与各 Skill 描述之间的
 * 匹配度，按相似度降序返回。不依赖外部向量数据库，纯本地计算。
 *
 * <p>相似度计算规则：
 * <ol>
 *   <li>将用户输入和 Skill 描述分别分词（中文 bigram，英文 whitespace）</li>
 *   <li>计算两个集合的 Jaccard 相似度：|A ∩ B| / |A ∪ B|</li>
 *   <li>低于 {@code similarityThreshold} 的 Skill 被过滤</li>
 * </ol>
 *
 * @author liziye
 * @since 0.3.0
 */
@Slf4j
public class SemanticSkillRouter implements SkillRouter {

    private final SkillProperties properties;

    public SemanticSkillRouter(SkillProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<ParsedSkill> match(String userInput, Collection<ParsedSkill> skills) {
        if (userInput == null || userInput.isBlank() || skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        double threshold = properties.getSimilarityThreshold();
        int maxResults = properties.getMaxMatchedSkills();

        Set<String> inputTokens = tokenize(userInput);

        return skills.stream()
                .map(skill -> new AbstractMap.SimpleEntry<>(
                        skill, computeSimilarity(inputTokens, skill)))
                .filter(e -> e.getValue() >= threshold)
                .sorted(Map.Entry.<ParsedSkill, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 计算用户输入与单个 Skill 的相似度。
     */
    private double computeSimilarity(Set<String> inputTokens, ParsedSkill skill) {
        if (skill.getDescription() == null || skill.getDescription().isBlank()) {
            // 降级：用 displayName 和 tags 计算
            return computeFallbackSimilarity(inputTokens, skill);
        }

        Set<String> descTokens = tokenize(skill.getDescription());

        // 主相似度：用户输入 vs 描述
        double descSim = jaccardSimilarity(inputTokens, descTokens);

        // 加成：名称和标签的相似度
        double nameSim = tokenize(skill.getDisplayName() != null
                ? skill.getDisplayName() : skill.getName())
                .stream()
                .anyMatch(inputTokens::contains) ? 0.1 : 0;

        double tagSim = 0;
        if (skill.getTags() != null && !skill.getTags().isEmpty()) {
            Set<String> tagTokens = tokenize(String.join(" ", skill.getTags()));
            tagSim = jaccardSimilarity(inputTokens, tagTokens) * 0.3;
        }

        // 优先级加成
        double priorityBonus = skill.getPriority() * 0.01;

        double finalScore = descSim + nameSim + tagSim + priorityBonus;
        log.debug("[SKILL-ROUTER] skill={} descSim={:.3f} nameSim={:.3f} tagSim={:.3f} priBonus={:.3f} final={:.3f}",
                skill.getName(), descSim, nameSim, tagSim, priorityBonus, finalScore);

        return finalScore;
    }

    /**
     * 降级相似度计算（Skill 没有描述时）。
     */
    private double computeFallbackSimilarity(Set<String> inputTokens, ParsedSkill skill) {
        Set<String> allTokens = new HashSet<>();
        if (skill.getDisplayName() != null) {
            allTokens.addAll(tokenize(skill.getDisplayName()));
        }
        allTokens.addAll(tokenize(skill.getName()));
        if (skill.getTags() != null) {
            allTokens.addAll(tokenize(String.join(" ", skill.getTags())));
        }
        if (skill.getCategory() != null) {
            allTokens.addAll(tokenize(skill.getCategory()));
        }
        return allTokens.isEmpty() ? 0 : jaccardSimilarity(inputTokens, allTokens);
    }

    /**
     * 分词：中文按 bigram，英文按空白分割。
     */
    static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        String lower = text.toLowerCase().trim();
        Set<String> tokens = new LinkedHashSet<>();

        // 中文字符用 bigram
        StringBuilder chineseBuffer = new StringBuilder();
        StringBuilder englishBuffer = new StringBuilder();

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isChinese(c)) {
                if (englishBuffer.length() > 0) {
                    tokens.addAll(splitEnglishTokens(englishBuffer.toString()));
                    englishBuffer.setLength(0);
                }
                chineseBuffer.append(c);
            } else {
                if (chineseBuffer.length() > 0) {
                    tokens.addAll(toBigrams(chineseBuffer.toString()));
                    chineseBuffer.setLength(0);
                }
                englishBuffer.append(c);
            }
        }

        // 处理剩余
        if (chineseBuffer.length() > 0) {
            tokens.addAll(toBigrams(chineseBuffer.toString()));
        }
        if (englishBuffer.length() > 0) {
            tokens.addAll(splitEnglishTokens(englishBuffer.toString()));
        }

        // 过滤纯空白 token
        tokens.removeIf(t -> t.isBlank());

        return tokens;
    }

    private static boolean isChinese(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
                || (c >= 0x2E80 && c <= 0xFE4F);
    }

    private static Set<String> toBigrams(String text) {
        Set<String> bigrams = new LinkedHashSet<>();
        for (int i = 0; i < text.length() - 1; i++) {
            bigrams.add(text.substring(i, i + 2));
        }
        return bigrams;
    }

    private static Set<String> splitEnglishTokens(String text) {
        return Stream.of(text.split("[\\s\\p{Punct}]+"))
                .filter(t -> t.length() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Jaccard 相似度：|A ∩ B| / |A ∪ B|
     */
    static double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }
}
