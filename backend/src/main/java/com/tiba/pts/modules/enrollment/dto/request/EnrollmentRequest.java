package com.tiba.pts.modules.enrollment.dto.request;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EnrollmentRequest {

  @NotNull(message = "ENROLLMENT_TYPE_REQUIRED")
  private EnrollmentType type;

  private String observation;

  @NotNull(message = "PROMOTION_ID_REQUIRED")
  private Long promotionId;

  @NotEmpty(message = "ENROLLMENT_SUBMITTED_DOCUMENTS_REQUIRED")
  private List<SubmittedDocumentRequest> enrollmentSubmittedDocuments;

  @NotNull(message = "STUDENT_DATA_REQUIRED")
  @Valid
  private StudentInfoRequest studentInfo;
}
