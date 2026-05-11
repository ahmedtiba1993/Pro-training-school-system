package com.tiba.pts.modules.trainingsession.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccreditedPromotionUpdateRequest(
    @NotBlank(message = "NAME_REQUIRED") String name,
    @NotNull(message = "REGISTRATION_OPENING_DATE_REQUIRED") LocalDate registrationOpeningDate,
    @NotNull(message = "REGISTRATION_DEADLINE_REQUIRED") LocalDate registrationDeadline,
    @NotNull(message = "REGISTRATION_FEE_REQUIRED")
        @DecimalMin(value = "0.0", inclusive = true, message = "FEE_MUST_BE_POSITIVE")
        BigDecimal registrationFee,
    @NotNull(message = "TUITION_FEE_REQUIRED")
        @DecimalMin(value = "0.0", inclusive = true, message = "FEE_MUST_BE_POSITIVE")
        BigDecimal tuitionFee,
    @NotNull(message = "CAPACITY_REQUIRED")
        @Min(value = 1, message = "CAPACITY_MUST_BE_STRICTLY_POSITIVE")
        Integer capacity) {

  public AccreditedPromotionUpdateRequest {
    if (name != null) {
      name = name.trim();
    }
  }

  @JsonIgnore
  @AssertTrue(message = "REGISTRATION_DEADLINE_MUST_BE_AFTER_OPENING_DATE")
  public boolean isDateRangeValid() {
    if (registrationOpeningDate == null || registrationDeadline == null) {
      return true;
    }
    return registrationDeadline.isAfter(registrationOpeningDate);
  }
}
