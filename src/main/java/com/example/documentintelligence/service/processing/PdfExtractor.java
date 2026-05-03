package com.example.documentintelligence.service.processing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfExtractor {

    /**
     * Extracts plain text from PDF bytes using PDFBox 3.x (Loader.loadPDF).
     * Throws PdfExtractionException when text is blank (image-only PDF) or
     * when PDFBox fails — consumer catches this to set Document status FAILED.
     */
    public String extract(byte[] pdfBytes) {
        PDDocument document = null;
        try {
            document = Loader.loadPDF(pdfBytes);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new PdfExtractionException(
                        "No extractable text found — PDF may be image-only or corrupted");
            }
            return text.trim();
        } catch (PdfExtractionException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfExtractionException(
                    "Failed to extract text from PDF: " + e.getMessage(), e);
        } finally {
            if (document != null) {
                try { document.close(); } catch (Exception ignored) {}
            }
        }
    }
}
