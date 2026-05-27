package com.liziye.spring.ai.lab.document.loader;

import com.liziye.spring.ai.lab.core.document.DocumentLoader;
import com.liziye.spring.ai.lab.core.exception.DocumentLoadException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 网页抓取加载器。
 *
 * <p>基于 Jsoup 实现网页内容抓取。支持 HTTP/HTTPS URL，自动跟随重定向。
 * 提取网页正文文本内容，并记录标题、URL 等元数据。
 *
 * <p>支持的类型：网页 URL（http/https）。
 * 伪扩展名 {@code "url"} 用于统一接口，实际通过 {@link #supports(String)} 方法判断。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.DocumentLoader} 接口。
 * 不支持从 {@code InputStream} 加载。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class WebPageLoader implements DocumentLoader {

    private static final int DEFAULT_TIMEOUT_MS = 30000;

    @Override
    public String[] supportedExtensions() {
        return new String[]{"url"};
    }

    @Override
    public List<Document> load(String path) throws DocumentLoadException {
        // path 可以是 URL 字符串
        if (!path.startsWith("http://") && !path.startsWith("https://")) {
            throw new DocumentLoadException(path, "Invalid URL: " + path + ". Must start with http:// or https://");
        }

        try {
            Connection connection = Jsoup.connect(path)
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .userAgent("SpringAILab/1.0")
                    .followRedirects(true);

            org.jsoup.nodes.Document jsoupDoc = connection.get();
            String text = jsoupDoc.body().text();

            if (text == null || text.isBlank()) {
                log.warn("Web page {} contains no text content", path);
                return Collections.emptyList();
            }

            log.info("Loaded web page: {}, chars={}", path, text.length());

            Document document = new Document(text);
            document.getMetadata().putAll(Map.of(
                    "source", path,
                    "type", "web",
                    "title", jsoupDoc.title(),
                    "url", path,
                    "char_count", String.valueOf(text.length())
            ));

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException(path, "Failed to load web page: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> load(InputStream inputStream, String extension) throws DocumentLoadException {
        throw new DocumentLoadException("input-stream",
                "WebPageLoader does not support loading from InputStream. Use load(String url) instead.");
    }

    @Override
    public boolean supports(String path) {
        return path.startsWith("http://") || path.startsWith("https://");
    }
}
