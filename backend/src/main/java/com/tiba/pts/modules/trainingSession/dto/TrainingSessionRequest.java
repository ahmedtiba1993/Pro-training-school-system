package com.tiba.pts.modules.trainingSession.dto;

import com.tiba.pts.modules.trainingSession.domain.enums.TrainingSessionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionRequest {

  @NotBlank(message = "PROMOTION NAME IS REQUIRED")
  private String promotionName;

  @NotNull(message = "ACADEMIC YEAR ID IS REQUIRED")
  private Long academicYearId;

  @NotNull(message = "EXPECTED START DATE IS REQUIRED")
  @FutureOrPresent(message = "EXPECTED START DATE CANNOT BE IN THE PAST")
  private LocalDate expectedStartDate;

  @NotNull(message = "ESTIMATED END DATE IS REQUIRED")
  private LocalDate estimatedEndDate;

  @NotNull(message = "STATUS IS REQUIRED")
  private TrainingSessionStatus status;

  @NotNull(message = "LEVEL ID IS REQUIRED")
  private Long levelId;

  @NotNull(message = "SPECIALTY ID IS REQUIRED")
  private Long specialtyId;

  @NotNull(message = "REGISTRATION OPEN DATE IS REQUIRED")
  private LocalDate registrationOpenDate;

  @NotNull(message = "REGISTRATION DEADLINE IS REQUIRED")
  private LocalDate registrationDeadline;

  @NotNull(message = "REGISTRATIONS OPEN IS REQUIRED")
  private Boolean registrationsOpen;

  @AssertTrue(message = "ESTIMATED END DATE MUST BE AFTER EXPECTED START DATE")
  private boolean isEndDateValid() {
    if (expectedStartDate == null || estimatedEndDate == null) {
      return true;
    }
    return estimatedEndDate.isAfter(expectedStartDate);
  }

  @AssertTrue(message = "REGISTRATION DEADLINE MUST BE AFTER REGISTRATION OPEN DATE")
  private boolean isRegistrationDeadlineValid() {
    if (registrationOpenDate == null || registrationDeadline == null) {
      return true;
    }
    return registrationDeadline.isAfter(registrationOpenDate);
  }
}
