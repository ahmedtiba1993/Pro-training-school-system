package com.tiba.pts.modules.trainingsession.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionSubjectResponse {

  private Long id;
  private String subjectName;
  private Double theoryHours;
  private Double practicalHours;
  private Double totalHours;
  private Double coefficient;
}
