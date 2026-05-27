package com.liziye.spring.ai.lab.document.loader;

import com.liziye.spring.ai.lab.core.document.DocumentLoader;
import com.liziye.spring.ai.lab.core.exception.DocumentLoadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PDF 文档加载器。
 *
 * <p>基于 Apache PDFBox 实现 PDF 文本提取。支持按位置排序提取文本，
 * 并记录页数、字符数等元数据。
 *
 * <p>支持的文件格式：{@code .pdf}。
 *
 * <p>实现 {@link com.liziye.spring.ai.lab.core.document.DocumentLoader} 接口。
 *
 * @author liziye
 * @since 1.0.0
 */
@Slf4j
public class PdfDocumentLoader implements DocumentLoader {

    @Override
    public String[] supportedExtensions() {
        return new String[]{"pdf"};
    }

    @Override
    public List<Document> load(String path) throws DocumentLoadException {
        File file = new File(path);
        if (!file.exists()) {
            throw new DocumentLoadException(path, "PDF file not found: " + path);
        }
        if (!file.canRead()) {
            throw new DocumentLoadException(path, "PDF file not readable: " + path);
        }

        try (PDDocument pdDocument = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(pdDocument);

            if (text == null || text.isBlank()) {
                log.warn("PDF document {} contains no text content", path);
                return Collections.emptyList();
            }

            int totalPages = pdDocument.getNumberOfPages();
            log.info("Loaded PDF: {}, pages={}, chars={}", path, totalPages, text.length());

            Document document = new Document(text);
            document.getMetadata().putAll(Map.of(
                    "source", path,
                    "type", "pdf",
                    "file_name", file.getName(),
                    "total_pages", String.valueOf(totalPages),
                    "char_count", String.valueOf(text.length())
            ));

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException(path, "Failed to load PDF document: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> load(InputStream inputStream, String extension) throws DocumentLoadException {
        try (PDDocument pdDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(pdDocument);

            Document document = new Document(text);
            document.getMetadata().put("type", "pdf");
            document.getMetadata().put("source", "input-stream");

            return Collections.singletonList(document);

        } catch (IOException e) {
            throw new DocumentLoadException("input-stream", "Failed to load PDF from stream", e);
        }
    }
}
