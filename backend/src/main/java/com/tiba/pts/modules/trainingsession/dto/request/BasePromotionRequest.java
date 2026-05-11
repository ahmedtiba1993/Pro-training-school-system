package com.tiba.pts.modules.trainingsession.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BasePromotionRequest {

  @NotBlank(message = "NAME_REQUIRED")
  private String name;

  @NotNull(message = "REGISTRATION_OPENING_DATE_REQUIRED")
  private LocalDate registrationOpeningDate;

  @NotNull(message = "REGISTRATION_DEADLINE_REQUIRED")
  private LocalDate registrationDeadline;

  @NotNull(message = "REGISTRATION_FEE_REQUIRED")
  @DecimalMin(value = "0.0", inclusive = true, message = "FEE_MUST_BE_POSITIVE")
  private BigDecimal registrationFee;

  @NotNull(message = "TUITION_FEE_REQUIRED")
  @DecimalMin(value = "0.0", inclusive = true, message = "FEE_MUST_BE_POSITIVE")
  private BigDecimal tuitionFee;

  @NotNull(message = "CAPACITY_REQUIRED")
  @Min(value = 1, message = "CAPACITY_MUST_BE_STRICTLY_POSITIVE")
  private Integer capacity;

  @NotNull(message = "TRAINING_ID_REQUIRED")
  private Long trainingId;

  // --- Surcharge des Setters pour le formatage (Trim & Majuscules) ---
  public void setName(String name) {
    this.name = (name != null) ? name.trim() : null;
  }

  // --- Validation Métier ---
  @JsonIgnore
  @AssertTrue(message = "REGISTRATION_DEADLINE_MUST_BE_AFTER_OPENING_DATE")
  public boolean isDateRangeValid() {
    if (registrationOpeningDate == null || registrationDeadline == null) {
      return true;
    }
    return registrationDeadline.isAfter(registrationOpeningDate);
  }
}
