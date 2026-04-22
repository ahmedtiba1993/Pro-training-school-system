package com.tiba.pts.modules.trainingsession.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AcceleratedPromotionResponse extends BasePromotionResponse {
  private Integer numberOfHours;
  private String level;
}
