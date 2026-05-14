package com.tiba.pts.core.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

  private final TemplateEngine templateEngine;

  /** Generates a PDF for Latin languages (French, English, etc.) - LTR Format */
  public byte[] generatePdf(String templateName, Map<String, Object> variables) {
    try {
      String htmlContent = processTemplate(templateName, variables);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();

      builder.useFastMode();
      builder.withHtmlContent(htmlContent, null);
      builder.toStream(outputStream);
      builder.run();

      return outputStream.toByteArray();

    } catch (Exception e) {
      log.error("Error generating standard PDF: {}", e.getMessage(), e);
      throw new RuntimeException("PDF_GENERATION_ERROR", e);
    }
  }

  /** Generates a PDF with Arabic support and connected characters - RTL Format */
  public byte[] generateArPdf(String templateName, Map<String, Object> variables) {
    try {
      String htmlContent = processTemplate(templateName, variables);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();

      // ENABLE ARABIC SUPPORT (RTL and connected characters)
      builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
      builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
      builder.defaultTextDirection(PdfRendererBuilder.TextDirection.RTL); // Force RTL direction

      // LOAD FONT
      builder.useFont(
          () -> {
            try {
              return new ClassPathResource("fonts/NotoSansArabic-Regular.ttf").getInputStream();
            } catch (IOException e) {
              throw new RuntimeException("FONT_LOAD_ERROR", e);
            }
          },
          "NotoSans");

      // Disable useFastMode() as it can interfere with complex text rendering like Arabic
      builder.withHtmlContent(htmlContent, null);
      builder.toStream(outputStream);
      builder.run();

      return outputStream.toByteArray();

    } catch (Exception e) {
      log.error("Error generating Arabic PDF: {}", e.getMessage(), e);
      throw new RuntimeException("PDF_GENERATION_ERROR", e);
    }
  }

  /** DRY: Centralized method to process the HTML template via Thymeleaf */
  private String processTemplate(String templateName, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process(templateName, context);
  }
}
