package com.liziye.spring.ai.lab.core.skill;

import com.liziye.spring.ai.lab.core.config.SkillProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Skill 加载器 — 扫描 skills 目录下的 .md 文件并进行解析和热加载。
 *
 * <p>支持两种目录来源：
 * <ul>
 *   <li><b>classpath</b>：{@code classpath:skills}，仅启动时扫描，不支持热加载</li>
 *   <li><b>文件系统</b>：{@code /path/to/skills} 或相对路径，支持 FileWatcher 热加载</li>
 * </ul>
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

    public SkillLoader(SkillProperties properties,
                       SkillRegistry registry,
                       ResourceLoader resourceLoader) {
        this.properties = properties;
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    // ===== 公开 API =====

    /**
     * 扫描并加载所有 Skill 文件，如果开启了热加载则启动文件监听。
     */
    public void load() {
        String dir = properties.getDirectory();

        if (isClasspathResource(dir)) {
            loadFromClasspath(dir);
        } else {
            loadFromFileSystem(dir);
            if (properties.isHotReload()) {
                startWatching();
            }
        }

        log.info("[SKILL-LOADER] Loaded {} skills from {}", registry.size(), dir);
    }

    /**
     * 停止热加载监听。
     */
    public void shutdown() {
        stopWatching();
    }

    /**
     * 手动重新加载单个 Skill 文件。
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

    // ===== 加载逻辑 =====

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

    private void loadFromFileSystem(String dir) {
        try {
            watchedDirectory = Paths.get(dir);
            if (!Files.isDirectory(watchedDirectory)) {
                log.warn("[SKILL-LOADER] Directory not found: {}. Creating...", dir);
                Files.createDirectories(watchedDirectory);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(watchedDirectory, "*.md")) {
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
        String raw = new String(resource.getInputStream().readAllBytes());
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
