package com.liziye.spring.ai.lab.document.loader;

import com.liziye.spring.ai.lab.core.document.DocumentLoader;
import com.liziye.spring.ai.lab.core.exception.DocumentLoadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Markdown 文档加载器。
 *
 * <p>支持的文件格式：{@code .md}、{@code .markdown}。
 * 从文件路径或输入流中加载 Markdown 文本为 {@code Document} 对象。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.DocumentLoader} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class MarkdownDocumentLoader implements DocumentLoader {

    @Override
    public String[] supportedExtensions() {
        return new String[]{"md", "markdown"};
    }

    @Override
    public List<Document> load(String path) throws DocumentLoadException {
        File file = new File(path);
        if (!file.exists()) {
            throw new DocumentLoadException(path, "Markdown file not found: " + path);
        }

        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            if (text.isBlank()) {
                log.warn("Markdown file {} is empty", path);
                return Collections.emptyList();
            }

            log.info("Loaded Markdown: {}, chars={}", path, text.length());

            Document document = new Document(text);
            document.getMetadata().putAll(Map.of(
                    "source", path,
                    "type", "markdown",
                    "file_name", file.getName(),
                    "char_count", String.valueOf(text.length())
            ));

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException(path, "Failed to load Markdown file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> load(InputStream inputStream, String extension) throws DocumentLoadException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String text = reader.lines().collect(Collectors.joining("\n"));

            Document document = new Document(text);
            document.getMetadata().put("type", "markdown");
            document.getMetadata().put("source", "input-stream");

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException("input-stream", "Failed to load Markdown from stream", e);
        }
    }
}
