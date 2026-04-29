package com.ahmed.pfa.cvplatform.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Extrait le texte brut des CV (PDF, DOCX, DOC) pour remplir {@code contenu_texte}.
 */
@Service
public class CvTextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(CvTextExtractionService.class);

    /** Limite proche de MySQL TEXT (65 Ko) — caractères Unicode. */
    private static final int MAX_TEXT_LENGTH = 60_000;

    public String extractPlainText(byte[] fileBytes, String originalFilename, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            return null;
        }
        String name = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";
        String mime = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";

        try {
            String raw;
            if (name.endsWith(".pdf") || mime.contains("pdf")) {
                raw = extractPdf(fileBytes);
            } else if (name.endsWith(".docx") || mime.contains("wordprocessingml") || mime.contains("officedocument.wordprocessingml")) {
                raw = extractDocx(fileBytes);
            } else if (name.endsWith(".doc") || mime.contains("msword")) {
                raw = extractDoc(fileBytes);
            } else {
                logger.debug("Format non géré pour extraction: {} ({})", originalFilename, contentType);
                return null;
            }
            return normalizeAndTruncate(raw);
        } catch (Exception e) {
            logger.warn("Extraction texte échouée pour {}: {}", originalFilename, e.getMessage());
            return null;
        }
    }

    private String extractPdf(byte[] data) throws IOException {
        try (PDDocument document = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
             XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extractDoc(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String normalizeAndTruncate(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replace('\u00A0', ' ')
                .replaceAll("\\R", "\n")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > MAX_TEXT_LENGTH) {
            return t.substring(0, MAX_TEXT_LENGTH);
        }
        return t;
    }
}
