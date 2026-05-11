package com.tiba.pts.modules.trainingsession.dto.response;

import com.tiba.pts.modules.trainingsession.domain.enums.DurationUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AcceleratedPromotionResponse extends BasePromotionResponse {

  private Integer durationValue;
  private DurationUnit durationUnit;
  private Integer capacity;
}
