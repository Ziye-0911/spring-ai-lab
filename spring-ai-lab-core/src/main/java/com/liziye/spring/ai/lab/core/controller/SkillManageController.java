package com.liziye.spring.ai.lab.core.controller;

import com.liziye.spring.ai.lab.core.skill.ParsedSkill;
import com.liziye.spring.ai.lab.core.skill.SkillLoader;
import com.liziye.spring.ai.lab.core.skill.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill 管理 REST API。
 *
 * <p>提供在运行时动态管理外部目录下 Skill 文件的能力，包括创建、更新、删除、重新加载。
 * 仅在 {@code spring.ai.lab.skill.enable-management=true} 时启用。
 *
 * <h3>端点列表</h3>
 * <ul>
 *   <li><b>GET</b> {@code /api/skills} — 列出所有 Skill</li>
 *   <li><b>GET</b> {@code /api/skills/{name}} — 获取 Skill 详情</li>
 *   <li><b>POST</b> {@code /api/skills/{name}} — 创建或更新 Skill</li>
 *   <li><b>DELETE</b> {@code /api/skills/{name}} — 删除外部 Skill</li>
 *   <li><b>POST</b> {@code /api/skills/reload} — 重新加载所有 Skill</li>
 * </ul>
 *
 * @author liziye
 * @since 0.3.1
 */
@Slf4j
@RestController
@RequestMapping("/api/skills")
public class SkillManageController {

    private final SkillRegistry registry;
    private final SkillLoader skillLoader;

    public SkillManageController(SkillRegistry registry, SkillLoader skillLoader) {
        this.registry = registry;
        this.skillLoader = skillLoader;
    }

    /**
     * 列出所有已加载的 Skill（摘要信息）。
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listSkills() {
        List<Map<String, Object>> skills = registry.getAll().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(m -> (int) m.getOrDefault("priority", 0),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", skills.size());
        result.put("externalDir", skillLoader.getExternalDirectory() != null
                ? skillLoader.getExternalDirectory().toString() : null);
        result.put("skills", skills);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取单个 Skill 的完整信息（包含 body 内容）。
     */
    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getSkill(@PathVariable String name) {
        Optional<ParsedSkill> opt = registry.get(name);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Skill not found: " + name));
        }

        ParsedSkill skill = opt.get();
        return ResponseEntity.ok(toDetail(skill));
    }

    /**
     * 创建或更新外部 Skill 文件。
     *
     * <p>请求体格式：YAML Frontmatter + Markdown 字符串
     *
     * <pre>{@code
     * POST /api/skills/my-skill
     * Content-Type: text/plain
     *
     * ---
     * name: my-skill
     * displayName: 我的技能
     * description: 技能描述
     * category: custom
     * priority: 3
     * ---
     * # 角色
     * 你是...系统提示词正文...
     * }</pre>
     */
    @PostMapping("/{name}")
    public ResponseEntity<Map<String, Object>> createOrUpdateSkill(
            @PathVariable String name,
            @RequestBody String content) {

        Path externalDir = skillLoader.getExternalDirectory();
        if (externalDir == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            "External skill directory is not configured. "
                                    + "Set spring.ai.lab.skill.external-dir in your application config."));
        }

        if (content == null || content.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Skill content must not be empty"));
        }

        try {
            // 确保文件名以 .md 结尾
            String filename = name.endsWith(".md") ? name : name + ".md";
            Path filePath = externalDir.resolve(filename);

            // 验证父目录路径安全性
            if (!filePath.normalize().startsWith(externalDir.normalize())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid skill name: potential path traversal"));
            }

            // 写入文件
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // 触发重新加载
            skillLoader.reload(filePath);

            log.info("[SKILL-API] Created/updated skill '{}' at {}", name, filePath);

            // 返回注册后的信息
            Optional<ParsedSkill> reloaded = registry.get(name);
            if (reloaded.isPresent()) {
                return ResponseEntity.ok(toDetail(reloaded.get()));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("name", name);
            result.put("message", "Skill written to file, but reload did not produce a valid skill");
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("[SKILL-API] Failed to write skill '{}': {}", name, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to write skill file: " + e.getMessage()));
        }
    }

    /**
     * 删除外部目录中的 Skill 文件。
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, Object>> deleteSkill(@PathVariable String name) {
        Path externalDir = skillLoader.getExternalDirectory();
        if (externalDir == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            "External skill directory is not configured. "
                                    + "Set spring.ai.lab.skill.external-dir in your application config."));
        }

        try {
            String filename = name.endsWith(".md") ? name : name + ".md";
            Path filePath = externalDir.resolve(filename);

            if (!filePath.normalize().startsWith(externalDir.normalize())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid skill name: potential path traversal"));
            }

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Skill file not found: " + filePath.getFileName()));
            }

            Files.delete(filePath);

            // 从注册中心注销
            registry.unregister(name);

            log.info("[SKILL-API] Deleted skill '{}' from {}", name, filePath);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("name", name);
            result.put("message", "Skill deleted");

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("[SKILL-API] Failed to delete skill '{}': {}", name, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete skill file: " + e.getMessage()));
        }
    }

    /**
     * 重新加载所有 Skill。先清空注册中心，再重新扫描外部目录和 classpath 兜底。
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadAll() {
        skillLoader.reloadAll();

        List<Map<String, Object>> skills = registry.getAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("total", skills.size());
        result.put("skills", skills);

        return ResponseEntity.ok(result);
    }

    // ===== 序列化辅助 =====

    private Map<String, Object> toSummary(ParsedSkill skill) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", skill.getName());
        map.put("displayName", skill.getDisplayName());
        map.put("description", skill.getDescription());
        map.put("category", skill.getCategory());
        map.put("priority", skill.getPriority());
        map.put("tags", skill.getTags());
        if (skill.getModel() != null) {
            Map<String, Object> modelMap = new LinkedHashMap<>();
            if (skill.getModel().getTemperature() != null) {
                modelMap.put("temperature", skill.getModel().getTemperature());
            }
            if (skill.getModel().getMaxTokens() != null) {
                modelMap.put("maxTokens", skill.getModel().getMaxTokens());
            }
            if (skill.getModel().getTopP() != null) {
                modelMap.put("topP", skill.getModel().getTopP());
            }
            if (!modelMap.isEmpty()) {
                map.put("model", modelMap);
            }
        }
        if (skill.getSourcePath() != null) {
            map.put("source", skill.getSourcePath().toString());
        }
        map.put("lastModified", skill.getLastModified());
        return map;
    }

    private Map<String, Object> toDetail(ParsedSkill skill) {
        Map<String, Object> map = toSummary(skill);
        map.put("body", skill.getBody());
        return map;
    }
}
