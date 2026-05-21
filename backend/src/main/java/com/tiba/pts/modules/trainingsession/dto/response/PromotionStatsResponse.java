package com.tiba.pts.modules.trainingsession.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionStatsResponse {

  private String promotionName;
  private String academicYearLabel;
  private Long totalSubjects;
  private Double totalCoefficient;
  private Double globalHourlyVolume;
  private String promotionType;
  private String trainingLabel;
}
