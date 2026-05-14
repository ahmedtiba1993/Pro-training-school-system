package com.tiba.pts.modules.enrollment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

  private Long existingStudentId;

  @Valid private StudentInfoRequest studentInfo;

  @JsonIgnore
  @AssertTrue(message = "YOU_MUST_PROVIDE_EITHER_EXISTING_ID_OR_NEW_STUDENT_INFO")
  public boolean isStudentDataProvided() {
    boolean hasExistingId = (existingStudentId != null);
    boolean hasNewStudent = (studentInfo != null);

    return hasExistingId ^ hasNewStudent;
  }
}
