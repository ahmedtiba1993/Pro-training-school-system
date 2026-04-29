package com.tiba.pts.modules.profiles.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.modules.profiles.domain.enums.ParentsSituation;
import com.tiba.pts.modules.profiles.domain.enums.StudentResidence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StudentRequest extends PersonRequest {

  @NotNull(message = "BIRTH_DATE_REQUIRED")
  private LocalDate birthDate;

  private String birthPlace;
  private String governorate;
  private String delegation;
  private String residenceAddress;
  private String correspondenceAddress;

  private StudentResidence residence;
  private ParentsSituation parentsSituation;
  private boolean hasChronicDisease;
  private String diseaseDescription;

  @Valid private List<StudentSiblingRequest> studentSiblings;

  @Valid private List<StudentParentRequest> parents;

  @JsonIgnore // To hide this field in Swagger documentation and the return JSON
  @AssertTrue(message = "EXACTLY_ONE_LEGAL_GUARDIAN_REQUIRED")
  public boolean isLegalGuardianValid() {
    // If the list of parents is empty or null, we let it pass
    // (if you want to force having parents, you can return false here)
    if (parents == null || parents.isEmpty()) {
      return true;
    }

    // We count how many parents have isLegalGuardian = true
    long guardianCount = parents.stream().filter(StudentParentInfoRequest::isLegalGuardian).count();

    // The validation passes ONLY if there is exactly 1 legal guardian
    return guardianCount == 1;
  }
}
