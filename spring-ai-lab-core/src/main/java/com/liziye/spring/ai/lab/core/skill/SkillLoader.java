package com.liziye.spring.ai.lab.core.skill;

import com.liziye.spring.ai.lab.core.config.SkillProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Skill 加载器 — 扫描 skills 目录下的 .md 文件并进行解析和热加载。
 *
 * <h3>多源加载策略</h3>
 * <ol>
 *   <li><b>外部文件目录</b>（{@code externalDir}）：优先级最高，支持热加载和 REST API 管理</li>
 *   <li><b>classpath 内置</b>（{@code classpath:skills}）：作为兜底默认，仅在外部目录未提供时生效</li>
 * </ol>
 *
 * <p>同名 Skill 以外部目录为准，可覆盖 classpath 内置 Skill。
 *
 * <h3>自动初始化</h3>
 * 当 {@code externalDir} 已配置且 {@code autoInit=true} 时，
 * 若外部目录为空或不存在，启动时自动从 classpath 复制内置 Skill 文件。
 *
 * <p>Skill 文件格式（YAML Frontmatter + Markdown）：
 * <pre>{@code
 * ---
 * name: weather-assistant
 * displayName: 天气助手
 * description: 提供天气查询、预报能力
 * category: utility
 * priority: 5
 * tags: [天气, 气象]
 * model:
 *   temperature: 0.3
 *   maxTokens: 1024
 * ---
 * # 角色
 * 你是一个专业的天气查询助手...
 * }</pre>
 *
 * @author liziye
 * @since 0.3.0
 */
@Slf4j
public class SkillLoader {

    private final SkillProperties properties;
    private final SkillRegistry registry;
    private final ResourceLoader resourceLoader;

    private final AtomicBoolean watching = new AtomicBoolean(false);
    private ExecutorService watchExecutor;
    private Path watchedDirectory;
    /**
     * 当前生效的外部目录路径（可能为 null）。
     */
    private Path externalDirectory;

    public SkillLoader(SkillProperties properties,
                       SkillRegistry registry,
                       ResourceLoader resourceLoader) {
        this.properties = properties;
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    // ===== 公开 API =====

    /**
     * 扫描并加载所有 Skill 文件。
     *
     * <p>加载顺序：
     * <ol>
     *   <li>若配置了 {@code externalDir} → 加载外部目录文件（同名 Skill 覆盖旧值）</li>
     *   <li>加载 classpath 内置 Skill 作为兜底（仅注册外部目录不存在的 Skill）</li>
     * </ol>
     */
    public void load() {
        boolean hasExternal = properties.getExternalDir() != null
                && !properties.getExternalDir().isBlank();

        if (hasExternal) {
            // 场景 1: 外部目录 + classpath 兜底
            loadExternalWithFallback();
        } else {
            // 场景 2: 仅 classpath 或仅文件系统（向后兼容旧逻辑）
            loadSingleSource(properties.getDirectory());
        }

        log.info("[SKILL-LOADER] Loaded {} skills from {}",
                registry.size(),
                hasExternal ? properties.getExternalDir() : properties.getDirectory());

        // 注册所有已加载 Skill 的摘要信息
        for (ParsedSkill skill : registry.getAll()) {
            log.debug("[SKILL-LOADER]   └── {} | {} | priority={}",
                    skill.getName(), skill.getDescription(), skill.getPriority());
        }
    }

    /**
     * 停止热加载监听。
     */
    public void shutdown() {
        stopWatching();
    }

    /**
     * 手动重新加载单个 Skill 文件（仅适用于外部目录）。
     *
     * @param filePath 文件路径
     */
    public void reload(Path filePath) {
        if (!Files.isRegularFile(filePath) || !filePath.toString().endsWith(".md")) {
            return;
        }
        try {
            ParsedSkill skill = parse(filePath);
            registry.register(skill);
            log.info("[SKILL-LOADER] Reloaded skill: {}", skill.getName());
        } catch (Exception e) {
            log.error("[SKILL-LOADER] Failed to reload: {}", filePath, e);
        }
    }

    /**
     * 重新加载外部目录的所有 Skill（用于 REST API 全量刷新）。
     * 先清空外部 Skill，再从外部目录重新加载，classpath 内置 Skill 作为兜底。
     */
    public void reloadAll() {
        if (externalDirectory == null) {
            log.warn("[SKILL-LOADER] No external directory configured, skipping reloadAll");
            return;
        }

        // 先清空所有 Skill
        registry.clear();

        // 重新加载：外部目录优先，classpath 兜底
        loadExternalWithFallback();
        log.info("[SKILL-LOADER] Full reload complete, {} skills loaded", registry.size());
    }

    /**
     * 获取外部目录路径。
     *
     * @return 外部目录路径，可能为 null
     */
    public Path getExternalDirectory() {
        return externalDirectory;
    }

    // ===== 多源加载逻辑 =====

    /**
     * 外部目录 + classpath 兜底 混合加载。
     */
    private void loadExternalWithFallback() {
        // Step 1: 初始化外部目录（可选自动种子化）
        initExternalDir();

        // Step 2: 加载外部目录 Skill（高优先级）
        loadFromFileSystem(externalDirectory);

        // Step 3: 加载 classpath 内置 Skill 作为兜底（仅注册外部不存在的）
        loadBuiltinFallback();

        // Step 4: 启用外部目录文件监听
        if (properties.isHotReload()) {
            watchedDirectory = externalDirectory;
            startWatching();
        }
    }

    /**
     * 单源加载（向后兼容旧逻辑）。
     */
    private void loadSingleSource(String dir) {
        if (isClasspathResource(dir)) {
            loadFromClasspath(dir);
        } else {
            watchedDirectory = Paths.get(dir);
            loadFromFileSystem(watchedDirectory);
            if (properties.isHotReload()) {
                startWatching();
            }
        }
    }

    /**
     * 初始化外部目录：创建目录 + 可选的自动种子化。
     */
    private void initExternalDir() {
        String dir = properties.getExternalDir();
        externalDirectory = Paths.get(dir);

        // 解析相对路径（相对于当前工作目录）
        if (!externalDirectory.isAbsolute()) {
            externalDirectory = Paths.get("").toAbsolutePath().resolve(externalDirectory).normalize();
        }

        try {
            if (!Files.exists(externalDirectory)) {
                Files.createDirectories(externalDirectory);
                log.info("[SKILL-LOADER] Created external directory: {}", externalDirectory);
            }
        } catch (IOException e) {
            log.error("[SKILL-LOADER] Failed to create external dir: {}", externalDirectory, e);
            return;
        }

        // 自动初始化：外部目录为空时，从 classpath 复制内置 Skill
        if (properties.isAutoInit() && isDirectoryEmpty(externalDirectory)) {
            seedFromClasspath(externalDirectory);
        }
    }

    /**
     * 从 classpath 复制内置 Skill 文件到目标目录。
     */
    private void seedFromClasspath(Path targetDir) {
        String builtinPattern = "classpath:skills/**/*.md";
        try {
            Resource[] resources =
                    ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                            .getResources(builtinPattern);
            if (resources.length == 0) {
                log.debug("[SKILL-LOADER] No builtin skills found on classpath for seeding");
                return;
            }

            for (Resource resource : resources) {
                if (!resource.exists()) continue;
                String filename = resource.getFilename();
                if (filename == null) continue;

                Path targetFile = targetDir.resolve(filename);
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("[SKILL-LOADER] Seeded builtin skill: {}", filename);
                }
            }

            log.info("[SKILL-LOADER] Auto-initialized {} builtin skills to external dir: {}",
                    resources.length, targetDir);
        } catch (IOException e) {
            log.warn("[SKILL-LOADER] Failed to seed builtin skills: {}", e.getMessage());
        }
    }

    /**
     * 加载 classpath 内置 Skill 作为兜底：仅注册外部目录不存在的同名 Skill。
     */
    private void loadBuiltinFallback() {
        Map<String, ParsedSkill> existing = getExistingSkillMap();

        String builtinPattern = "classpath:skills/**/*.md";
        try {
            Resource[] resources =
                    ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                            .getResources(builtinPattern);
            for (Resource resource : resources) {
                try {
                    if (!resource.exists()) continue;
                    ParsedSkill skill = parse(resource);
                    if (!existing.containsKey(skill.getName())) {
                        registry.register(skill);
                        log.debug("[SKILL-LOADER] Loaded builtin fallback: {}", skill.getName());
                    } else {
                        log.debug("[SKILL-LOADER] Skipped builtin (overridden by external): {}",
                                skill.getName());
                    }
                } catch (Exception e) {
                    log.error("[SKILL-LOADER] Failed to parse builtin skill: {}",
                            resource.getDescription(), e);
                }
            }
        } catch (IOException e) {
            log.debug("[SKILL-LOADER] No builtin skills found at classpath:skills");
        }
    }

    private Map<String, ParsedSkill> getExistingSkillMap() {
        Map<String, ParsedSkill> map = new HashMap<>();
        for (ParsedSkill skill : registry.getAll()) {
            map.put(skill.getName(), skill);
        }
        return map;
    }

    private boolean isDirectoryEmpty(Path dir) {
        try {
            if (!Files.isDirectory(dir)) return true;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                return !stream.iterator().hasNext();
            }
        } catch (IOException e) {
            return true;
        }
    }

    // ===== 文件系统加载 =====

    private void loadFromFileSystem(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                log.warn("[SKILL-LOADER] Directory not found: {}. Creating...", dir);
                Files.createDirectories(dir);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.md")) {
                for (Path path : stream) {
                    try {
                        ParsedSkill skill = parse(path);
                        registry.register(skill);
                        log.debug("[SKILL-LOADER] Loaded skill: {}", skill.getName());
                    } catch (Exception e) {
                        log.error("[SKILL-LOADER] Failed to parse: {}", path, e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("[SKILL-LOADER] Failed to load skills from: {}", dir, e);
        }
    }

    // ===== Classpath 加载 =====

    private boolean isClasspathResource(String dir) {
        return dir.startsWith("classpath:");
    }

    private void loadFromClasspath(String dir) {
        String pattern = dir + "/**/*.md";
        try {
            Resource[] resources =
                    ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                            .getResources(pattern);
            for (Resource resource : resources) {
                try {
                    if (!resource.exists()) continue;
                    ParsedSkill skill = parse(resource);
                    registry.register(skill);
                    log.debug("[SKILL-LOADER] Loaded skill from classpath: {}", skill.getName());
                } catch (Exception e) {
                    log.error("[SKILL-LOADER] Failed to parse classpath skill: {}",
                            resource.getDescription(), e);
                }
            }
        } catch (IOException e) {
            log.warn("[SKILL-LOADER] No skills found at classpath: {}. Skipping skill loading.", dir);
        }
    }

    // ===== 解析逻辑 =====

    /**
     * 解析 .md 文件。
     */
    ParsedSkill parse(Path filePath) throws IOException {
        String raw = Files.readString(filePath);
        return parseContent(raw, filePath);
    }

    /**
     * 解析 classpath 资源。
     */
    ParsedSkill parse(Resource resource) throws IOException {
        String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return parseContent(raw, Path.of(resource.getFilename() != null
                ? resource.getFilename() : "unknown.md"));
    }

    /**
     * 解析 Skill 内容（YAML Frontmatter + Markdown Body）。
     */
    ParsedSkill parseContent(String raw, Path sourcePath) {
        String name = stripExtension(sourcePath.getFileName().toString());

        if (raw.startsWith("---")) {
            // 有 YAML frontmatter
            int secondDelimiter = raw.indexOf("---", 3);
            if (secondDelimiter > 0) {
                String yamlSection = raw.substring(3, secondDelimiter).trim();
                String bodySection = raw.substring(secondDelimiter + 3).trim();

                Yaml yaml = new Yaml();
                Map<String, Object> metadata = yaml.load(yamlSection);

                if (metadata != null) {
                    return buildFromMetadata(name, metadata, bodySection, sourcePath);
                }
            }
        }

        // 无 frontmatter，整篇作为 body
        return ParsedSkill.builder()
                .name(name)
                .displayName(name)
                .description("")
                .body(raw.trim())
                .sourcePath(sourcePath)
                .lastModified(System.currentTimeMillis())
                .build();
    }

    @SuppressWarnings("unchecked")
    private ParsedSkill buildFromMetadata(String fallbackName,
                                          Map<String, Object> metadata,
                                          String body,
                                          Path sourcePath) {
        String name = (String) metadata.getOrDefault("name", fallbackName);
        String displayName = (String) metadata.getOrDefault("displayName", name);
        String description = (String) metadata.getOrDefault("description", "");
        String category = (String) metadata.getOrDefault("category", "general");
        int priority = metadata.get("priority") instanceof Number
                ? ((Number) metadata.get("priority")).intValue() : 0;

        List<String> tags = null;
        if (metadata.get("tags") instanceof List) {
            tags = ((List<Object>) metadata.get("tags")).stream()
                    .map(Object::toString)
                    .toList();
        }

        ParsedSkill.ModelConfig modelConfig = null;
        if (metadata.get("model") instanceof Map) {
            Map<String, Object> modelMap = (Map<String, Object>) metadata.get("model");
            modelConfig = ParsedSkill.ModelConfig.builder()
                    .temperature(modelMap.get("temperature") instanceof Number
                            ? ((Number) modelMap.get("temperature")).doubleValue() : null)
                    .maxTokens(modelMap.get("maxTokens") instanceof Number
                            ? ((Number) modelMap.get("maxTokens")).intValue() : null)
                    .topP(modelMap.get("topP") instanceof Number
                            ? ((Number) modelMap.get("topP")).doubleValue() : null)
                    .build();
        }

        return ParsedSkill.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .category(category)
                .tags(tags)
                .priority(priority)
                .model(modelConfig)
                .body(body)
                .sourcePath(sourcePath)
                .lastModified(System.currentTimeMillis())
                .rawMetadata(metadata)
                .build();
    }

    // ===== 文件监听（热加载） =====

    private void startWatching() {
        if (watchedDirectory == null || !Files.isDirectory(watchedDirectory)) {
            log.warn("[SKILL-LOADER] Cannot watch null directory");
            return;
        }

        if (watching.compareAndSet(false, true)) {
            watchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "skill-watcher");
                t.setDaemon(true);
                return t;
            });

            watchExecutor.submit(this::watchLoop);
            log.info("[SKILL-LOADER] Started file watcher on: {}", watchedDirectory);
        }
    }

    private void stopWatching() {
        if (watching.compareAndSet(true, false)) {
            if (watchExecutor != null) {
                watchExecutor.shutdownNow();
                watchExecutor = null;
            }
            log.info("[SKILL-LOADER] Stopped file watcher");
        }
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            watchedDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (watching.get()) {
                WatchKey key;
                try {
                    key = watchService.poll(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    Path fileName = (Path) event.context();
                    Path fullPath = watchedDirectory.resolve(fileName);

                    if (!fileName.toString().endsWith(".md")) continue;

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE
                            || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // 防抖：等文件写入完成
                        sleep(200);
                        reload(fullPath);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        String name = stripExtension(fileName.toString());
                        registry.unregister(name);
                        log.info("[SKILL-LOADER] Unregistered skill: {}", name);
                    }
                }

                if (!key.reset()) break;
            }
        } catch (IOException e) {
            log.error("[SKILL-LOADER] WatchService error: {}", e.getMessage(), e);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
