package com.tiba.pts.modules.trainingsession.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionSubjectRequest {

  @NotNull(message = "PROMOTION_ID_REQUIRED")
  private Long promotionId;

  @NotNull(message = "SUBJECT_ID_REQUIRED")
  private Long subjectId;

  private Long academicPeriodId;

  @NotNull(message = "COEFFICIENT_REQUIRED")
  @Positive(message = "COEFFICIENT_MUST_BE_POSITIVE")
  private Double coefficient;
}
