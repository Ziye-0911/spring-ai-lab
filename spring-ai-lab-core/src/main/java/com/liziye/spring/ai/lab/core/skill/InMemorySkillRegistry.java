package com.liziye.spring.ai.lab.core.skill;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的 Skill 注册中心实现。
 *
 * <p>线程安全，适用于单节点部署。
 *
 * @author liziye
 * @since 0.3.0
 */
public class InMemorySkillRegistry implements SkillRegistry {

    private final Map<String, ParsedSkill> skills = new ConcurrentHashMap<>();

    @Override
    public void register(ParsedSkill skill) {
        skills.put(skill.getName(), skill);
    }

    @Override
    public void unregister(String name) {
        skills.remove(name);
    }

    @Override
    public Optional<ParsedSkill> get(String name) {
        return Optional.ofNullable(skills.get(name));
    }

    @Override
    public Collection<ParsedSkill> getAll() {
        return Collections.unmodifiableCollection(skills.values());
    }

    @Override
    public List<ParsedSkill> getByCategory(String category) {
        return skills.values().stream()
                .filter(s -> category.equalsIgnoreCase(s.getCategory()))
                .sorted(Comparator.comparingInt(ParsedSkill::getPriority).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<ParsedSkill> getByTag(String tag) {
        return skills.values().stream()
                .filter(s -> s.getTags() != null && s.getTags().stream()
                        .anyMatch(t -> t.equalsIgnoreCase(tag)))
                .sorted(Comparator.comparingInt(ParsedSkill::getPriority).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public int size() {
        return skills.size();
    }

    @Override
    public void clear() {
        skills.clear();
    }
}
