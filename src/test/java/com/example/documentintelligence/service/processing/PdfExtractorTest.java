package com.example.documentintelligence.service.processing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfExtractorTest {

    private final PdfExtractor extractor = new PdfExtractor();

    @Test
    void extractsTextFromValidPdf() throws Exception {
        byte[] pdf = pdfWithText("Hello from PDFBox");
        String result = extractor.extract(pdf);
        assertThat(result).contains("Hello from PDFBox");
    }

    @Test
    void throwsOnEmptyBytes() {
        assertThatThrownBy(() -> extractor.extract(new byte[0]))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("Failed");
    }

    @Test
    void throwsOnCorruptedBytes() {
        assertThatThrownBy(() -> extractor.extract("not a pdf".getBytes()))
                .isInstanceOf(PdfExtractionException.class);
    }

    @Test
    void throwsOnBlankPdfWithNoTextLayer() throws Exception {
        byte[] pdf = blankPdf();
        assertThatThrownBy(() -> extractor.extract(pdf))
                .isInstanceOf(PdfExtractionException.class)
                .hasMessageContaining("No extractable text");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static byte[] pdfWithText(String content) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(content);
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static byte[] blankPdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage()); // empty page — no content stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
