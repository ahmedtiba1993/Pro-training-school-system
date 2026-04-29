package com.tiba.pts.modules.enrollment.dto.response;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentType;
import lombok.Data;

import java.util.List;

@Data
public class EnrollmentResponse {
  private Long id;
  private String enrollmentNumber;
  private EnrollmentType type;
  private EnrollmentStatus status;
  private String observation;

  private PromotionSummaryResponse promotion;
  private StudentSummaryResponse student;
  private List<SubmittedDocumentResponse> enrollmentSubmittedDocuments;
}
