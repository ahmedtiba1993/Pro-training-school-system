package com.tiba.pts.modules.academicyear.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ActiveAcademicYearDTO {

  private String label;

  private LocalDate startDate;

  private LocalDate endDate;

  private String currentTerm;

  private long daysRemaining;
}
