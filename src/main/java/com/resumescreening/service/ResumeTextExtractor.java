package com.resumescreening.service;

import com.resumescreening.exception.InvalidFileException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * Service responsible for extracting plain text from PDF and DOCX resume files
 * using Apache PDFBox and Apache POI respectively.
 */
@Service
public class ResumeTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResumeTextExtractor.class);

    /**
     * Extract text from a PDF file input stream.
     */
    public String extractFromPdf(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.debug("PDF text extraction complete, length: {}", text.length());
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            throw new InvalidFileException("Could not read PDF file. Please ensure it is not corrupted or password-protected.");
        }
    }

    /**
     * Extract text from a DOCX file input stream.
     */
    public String extractFromDocx(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            String text = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
            log.debug("DOCX text extraction complete, length: {}", text.length());
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from DOCX: {}", e.getMessage());
            throw new InvalidFileException("Could not read DOCX file. Please ensure it is a valid Word document.");
        }
    }

    /**
     * Extract text from either PDF or DOCX based on file type string.
     */
    public String extract(InputStream inputStream, String fileType) {
        return switch (fileType.toUpperCase()) {
            case "PDF"  -> extractFromPdf(inputStream);
            case "DOCX" -> extractFromDocx(inputStream);
            default     -> throw new InvalidFileException("Unsupported file type: " + fileType);
        };
    }
}
