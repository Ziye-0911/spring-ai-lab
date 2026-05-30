package com.liziye.spring.ai.lab.core.skill;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Skill 注册中心 — 管理所有已加载的 Skill。
 *
 * <p>提供按名称、标签、分类维度的查询能力。
 *
 * @author liziye
 * @since 0.3.0
 */
public interface SkillRegistry {

    /**
     * 注册一个 Skill。
     *
     * @param skill 已解析的 Skill
     */
    void register(ParsedSkill skill);

    /**
     * 注销一个 Skill。
     *
     * @param name Skill 名称
     */
    void unregister(String name);

    /**
     * 按名称获取 Skill。
     *
     * @param name Skill 名称
     * @return Skill（可能存在为空）
     */
    Optional<ParsedSkill> get(String name);

    /**
     * 获取所有 Skill。
     *
     * @return 所有已注册的 Skill
     */
    Collection<ParsedSkill> getAll();

    /**
     * 按分类获取 Skill。
     *
     * @param category 分类名称
     * @return 匹配的 Skill 列表
     */
    List<ParsedSkill> getByCategory(String category);

    /**
     * 按标签获取 Skill。
     *
     * @param tag 标签名称
     * @return 匹配的 Skill 列表
     */
    List<ParsedSkill> getByTag(String tag);

    /**
     * 获取注册的 Skill 总数。
     *
     * @return Skill 数量
     */
    int size();

    /**
     * 清空所有 Skill。
     */
    void clear();
}
