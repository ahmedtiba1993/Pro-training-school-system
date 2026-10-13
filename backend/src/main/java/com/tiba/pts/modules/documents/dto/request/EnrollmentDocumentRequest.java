package com.tiba.pts.modules.documents.dto.request;

import com.tiba.pts.modules.documents.domain.enums.DocumentCondition;
import com.tiba.pts.modules.documents.domain.enums.DocumentNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public record EnrollmentDocumentRequest(
    @NotBlank(message = "DOCUMENTS_LABEL_REQUIRED")
    String label,

    @NotBlank(message = "DOCUMENTS_LABEL_AR_REQUIRED")
    String labelAr,

    @NotBlank(message = "DOCUMENTS_CODE_REQUIRED")
    String code,

    @NotNull(message = "DOCUMENTS_QUANTITY_REQUIRED")
    @Positive(message = "DOCUMENTS_QUANTITY_MUST_BE_POSITIVE")
    Integer quantity,

    @NotNull(message = "DOCUMENTS_NATURE_REQUIRED")
    DocumentNature nature,

    @NotNull(message = "DOCUMENTS_CONDITION_REQUIRED")
    DocumentCondition condition,

    @NotEmpty(message = "DOCUMENTS_LEVEL_IDS_REQUIRED")
    Set<Long> levelIds,

    @NotNull(message = "DOCUMENTS_MANDATORY_REQUIRED")
    Boolean mandatory
) {
  public EnrollmentDocumentRequest {
    if (label != null) {
      label = label.trim();
    }
    if (code != null) {
      code = normalizeCode(code);
    }
  }

  private static String normalizeCode(String input) {
    if (input == null) return null;

    return StringUtils.stripAccents(input)
        .toUpperCase()
        .trim()
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");
  }
}
