package com.tiba.pts.modules.trainingsession.dto.response;

import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OngoingAcceleratedPromotionResponse {
  private Long id;
  private String name;
  private String code;
  private LocalDate startDate;
  private LocalDate endDate;
  private PromotionStatus status;
  private Integer enrollmentCount;
  private Integer capacity;
}
