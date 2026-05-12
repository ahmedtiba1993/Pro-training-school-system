package com.tiba.pts.core.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

  private final TemplateEngine templateEngine;

  public byte[] generateArPdf(String templateName, Map<String, Object> variables) {
    try {
      Context context = new Context();
      context.setVariables(variables);
      String htmlContent = templateEngine.process(templateName, context);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();

      // ENABLE ARABIC SUPPORT (RTL and connected characters)
      builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
      builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
      builder.defaultTextDirection(PdfRendererBuilder.TextDirection.RTL); // Force RTL direction

      // LOAD FONT
      // Use getInputStream() to ensure it works correctly when the project is compiled into a .jar
      // file
      builder.useFont(
          () -> {
            try {
              return new ClassPathResource("fonts/NotoSansArabic-Regular.ttf").getInputStream();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          },
          "NotoSans");

      // Disable useFastMode() as it can interfere with complex text rendering like Arabic
      builder.withHtmlContent(htmlContent, null);
      builder.toStream(outputStream);
      builder.run();

      return outputStream.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException("PDF_GENERATION_ERROR", e);
    }
  }
}
