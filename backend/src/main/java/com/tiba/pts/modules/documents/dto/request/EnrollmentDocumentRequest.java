package com.tiba.pts.modules.documents.dto.request;

import com.tiba.pts.modules.documents.domain.enums.DocumentCondition;
import com.tiba.pts.modules.documents.domain.enums.DocumentNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class EnrollmentDocumentRequest {
  @NotBlank(message = "NAME_REQUIRED")
  private String name;

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
}
