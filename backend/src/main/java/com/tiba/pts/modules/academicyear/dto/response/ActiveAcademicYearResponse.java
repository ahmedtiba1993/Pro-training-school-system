package com.tiba.pts.modules.academicyear.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ActiveAcademicYearResponse {

  private String label;

  private LocalDate startDate;

  private LocalDate endDate;

  private String currentPeriod;

  private long daysRemaining;
}
