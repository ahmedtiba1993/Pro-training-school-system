package com.tiba.pts.modules.enrollment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EnrollmentRequest {

  @Valid
  @NotNull(message = "STUDENT_REQUIRED")
  private StudentDto student;

  @Valid private ParentDto father;

  @Valid private ParentDto mother;

  @Valid private ParentDto guardian;

  @NotNull(message = "TRAINING_SESSION_ID_REQUIRED")
  private Long trainingSessionId;

  @Valid private List<ProvidedDocumentDto> documents;

  /**
   * This method is automatically called by Spring during validation (@Valid). If it returns
   * "false", the "EXACTLY_ONE_LEGAL_GUARDIAN_REQUIRED" error will be triggered. @JsonIgnore
   * prevents this method from appearing in JSON responses.
   */
  @JsonIgnore
  @AssertTrue(message = "EXACTLY_ONE_LEGAL_GUARDIAN_REQUIRED")
  public boolean isValidLegalGuardianCount() {
    int count = 0;

    if (father != null && Boolean.TRUE.equals(father.getIsLegalGuardian())) count++;
    if (mother != null && Boolean.TRUE.equals(mother.getIsLegalGuardian())) count++;
    if (guardian != null && Boolean.TRUE.equals(guardian.getIsLegalGuardian())) count++;

    // Validation succeeds only if the count is exactly 1
    return count == 1;
  }
}
