package com.tiba.pts.modules.trainingsession.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AccreditedPromotionRequest extends BasePromotionRequest {

  @NotNull(message = "REGISTRATION_OPENING_DATE_IS_REQUIRED")
  private LocalDate registrationOpeningDate;

  @NotNull(message = "REGISTRATION_DEADLINE_IS_REQUIRED")
  private LocalDate registrationDeadline;

  @NotNull(message = "ACADEMIC_YEAR_ID_IS_REQUIRED")
  private Long academicYearId;

  @AssertTrue(message = "REGISTRATION_DEADLINE_MUST_BE_AFTER_OPENING_DATE")
  private boolean isRegistrationDeadlineValid() {
    if (registrationOpeningDate == null || registrationDeadline == null) {
      return true;
    }
    return registrationDeadline.isAfter(registrationOpeningDate);
  }
}
