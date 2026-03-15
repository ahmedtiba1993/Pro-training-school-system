package com.tiba.pts.modules.trainingSession.dto;

import com.tiba.pts.modules.trainingSession.domain.enums.TrainingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionResponse {

  private Long id;
  private String promotionName;

  private Long academicYearId;
  private String academicYearLabel;

  private Integer enrolledCount;
  private LocalDate expectedStartDate;
  private LocalDate estimatedEndDate;
  private TrainingSessionStatus status;

  private Long levelId;
  private String levelCode;

  private Long specialtyId;
  private String specialtyCode;

  private LocalDate registrationOpenDate;
  private LocalDate registrationDeadline;
  private Boolean registrationsOpen;
}
