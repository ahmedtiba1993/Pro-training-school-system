package com.tiba.pts.modules.trainingsession.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContinuousPromotionRequest extends BasePromotionRequest {

  @NotNull(message = "DURATION_IS_REQUIRED")
  @Positive(message = "DURATION_MUST_BE_POSITIVE")
  private Integer duration;
}
