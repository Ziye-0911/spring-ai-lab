package com.liziye.spring.ai.lab.document.loader;

import com.liziye.spring.ai.lab.core.document.DocumentLoader;
import com.liziye.spring.ai.lab.core.exception.DocumentLoadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Word 文档加载器。
 *
 * <p>基于 Apache POI 实现 {@code .docx} 文档文本提取。
 * 使用 {@code XWPFWordExtractor} 提取完整文本内容。
 *
 * <p>支持的文件格式：{@code .docx}。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.DocumentLoader} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class WordDocumentLoader implements DocumentLoader {

    @Override
    public String[] supportedExtensions() {
        return new String[]{"docx"};
    }

    @Override
    public List<Document> load(String path) throws DocumentLoadException {
        File file = new File(path);
        if (!file.exists()) {
            throw new DocumentLoadException(path, "Word file not found: " + path);
        }

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument xwpfDocument = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(xwpfDocument)) {

            String text = extractor.getText();

            if (text == null || text.isBlank()) {
                log.warn("Word document {} contains no text content", path);
                return Collections.emptyList();
            }

            log.info("Loaded Word: {}, chars={}", path, text.length());

            Document document = new Document(text);
            document.getMetadata().putAll(Map.of(
                    "source", path,
                    "type", "docx",
                    "file_name", file.getName(),
                    "char_count", String.valueOf(text.length())
            ));

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException(path, "Failed to load Word document: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> load(InputStream inputStream, String extension) throws DocumentLoadException {
        try (XWPFDocument xwpfDocument = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(xwpfDocument)) {

            String text = extractor.getText();

            Document document = new Document(text);
            document.getMetadata().put("type", "docx");
            document.getMetadata().put("source", "input-stream");

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException("input-stream", "Failed to load Word from stream", e);
        }
    }
}
