package com.tiba.pts.modules.trainingsession.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AcceleratedPromotionRequest extends BasePromotionRequest {

  @NotNull(message = "NUMBER_OF_HOURS_IS_REQUIRED")
  @Positive(message = "NUMBER_OF_HOURS_MUST_BE_POSITIVE")
  private Integer numberOfHours;

  private String level;
}
