package com.liziye.spring.ai.lab.core.skill;

import java.util.Collection;
import java.util.List;

/**
 * Skill 路由器 — 根据用户输入匹配最合适的 Skill。
 *
 * <p>支持多种路由策略：语义匹配、关键词、LLM 路由等。
 *
 * @author liziye
 * @since 0.3.0
 */
public interface SkillRouter {

    /**
     * 根据用户输入匹配 Skill。
     *
     * @param userInput 用户输入文本
     * @param skills    候选 Skill 集合
     * @return 按相关度降序排列的 Skill 列表
     */
    List<ParsedSkill> match(String userInput, Collection<ParsedSkill> skills);
}
