package com.tiba.pts.modules.trainingsession.dto.response;

import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccreditedPromotionResponse extends BasePromotionResponse {
  private LocalDate registrationOpeningDate;
  private LocalDate registrationDeadline;

  // Useful information about the academic year
  private Long academicYearId;
  private String academicYearLabel;
}
