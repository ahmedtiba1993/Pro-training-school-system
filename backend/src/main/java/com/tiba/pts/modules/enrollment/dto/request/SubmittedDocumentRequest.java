package com.tiba.pts.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmittedDocumentRequest {

  @NotNull(message = "ENROLLMENT_DOCUMENT_ID_REQUIRED")
  private Long enrollmentDocumentId;

  @NotNull(message = "PROVIDED_STATUS_REQUIRED")
  private Boolean provided;
}
