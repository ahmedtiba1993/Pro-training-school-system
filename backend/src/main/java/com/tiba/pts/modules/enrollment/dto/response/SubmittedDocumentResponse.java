package com.tiba.pts.modules.enrollment.dto.response;

import lombok.Data;

@Data
public class SubmittedDocumentResponse {
  private Long id;
  private Long enrollmentDocumentId;
  private String documentName;
  private boolean provided;
}
