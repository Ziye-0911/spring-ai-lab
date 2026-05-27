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
 * 纯文本加载器。
 *
 * <p>支持广泛的文本文件格式：{@code .txt}、{@code .log}、{@code .csv}、
 * {@code .properties}、{@code .yaml}、{@code .yml}、{@code .xml}、{@code .json}、
 * {@code .java}、{@code .py}、{@code .js}、{@code .ts}、{@code .jsx}、{@code .tsx}、
 * {@code .css}、{@code .html}、{@code .htm}、{@code .sql}、{@code .sh}、{@code .bat}、
 * {@code .ps1}。
 * 使用 UTF-8 编码读取，将文件扩展名作为文档类型保存到元数据中。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.DocumentLoader} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class TxtDocumentLoader implements DocumentLoader {

    @Override
    public String[] supportedExtensions() {
        return new String[]{
                "txt", "log", "csv",
                "properties", "yaml", "yml", "xml", "json",
                "java", "py", "js", "ts", "jsx", "tsx",
                "css", "html", "htm", "sql", "sh", "bat", "ps1"
        };
    }

    @Override
    public List<Document> load(String path) throws DocumentLoadException {
        File file = new File(path);
        if (!file.exists()) {
            throw new DocumentLoadException(path, "Text file not found: " + path);
        }

        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            if (text.isBlank()) {
                log.warn("Text file {} is empty", path);
                return Collections.emptyList();
            }

            log.info("Loaded text: {}, chars={}", path, text.length());

            String ext = getFileExtension(path);
            Document document = new Document(text);
            document.getMetadata().putAll(Map.of(
                    "source", path,
                    "type", ext,
                    "file_name", file.getName(),
                    "char_count", String.valueOf(text.length())
            ));

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException(path, "Failed to load text file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> load(InputStream inputStream, String extension) throws DocumentLoadException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String text = reader.lines().collect(Collectors.joining("\n"));

            Document document = new Document(text);
            document.getMetadata().put("type", extension);
            document.getMetadata().put("source", "input-stream");

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException("input-stream", "Failed to load text from stream", e);
        }
    }
}
