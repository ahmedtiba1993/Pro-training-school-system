package com.tiba.pts.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProvidedDocumentDto {

  @NotNull(message = "DOCUMENT_ID_REQUIRED")
  private Long registrationDocumentId;

  @NotNull(message = "IS_PROVIDED_STATUS_REQUIRED")
  private Boolean isProvided;
}
