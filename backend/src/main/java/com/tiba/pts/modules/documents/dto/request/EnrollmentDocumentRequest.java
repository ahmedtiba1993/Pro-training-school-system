package com.tiba.pts.modules.documents.dto.request;

import com.tiba.pts.modules.documents.domain.enums.DocumentCondition;
import com.tiba.pts.modules.documents.domain.enums.DocumentNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

@Data
public class EnrollmentDocumentRequest {

  @NotBlank(message = "LABEL_REQUIRED")
  private String label;

  private String labelAr;

  @NotBlank(message = "CODE_REQUIRED")
  private String code;

  @NotNull(message = "QUANTITY_REQUIRED")
  private Integer quantity;

  @NotNull(message = "TYPE_REQUIRED")
  private DocumentNature nature;

  @NotNull(message = "CONDITION_REQUIRED")
  private DocumentCondition condition;

  @NotEmpty(message = "LEVEL_IDS_REQUIRED")
  private Set<Long> levelIds;

  @NotNull(message = "MANDATORY_REQUIRED")
  private Boolean mandatory;

  public void normalizeData() {
    // Nettoyage du label
    if (this.label != null) {
      this.label = this.label.trim();
    }

    // Formatage strict
    this.code = normalizeCode(this.code);
  }

  private String normalizeCode(String input) {
    if (input == null) return null;

    return StringUtils.stripAccents(input)
        .toUpperCase()
        .trim()
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");
  }
}
