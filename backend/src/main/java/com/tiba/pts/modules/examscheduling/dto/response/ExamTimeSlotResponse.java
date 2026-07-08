package com.tiba.pts.modules.examscheduling.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTimeSlotResponse {
  private Long id;
  private String code;
  private String label;
  private LocalTime startTime;
  private LocalTime endTime;
  private LocalDateTime createdDate;
  private LocalDateTime modifiedDate;
  private Boolean isActive;
}
