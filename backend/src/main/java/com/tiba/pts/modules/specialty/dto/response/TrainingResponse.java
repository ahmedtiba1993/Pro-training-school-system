package com.tiba.pts.modules.specialty.dto.response;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import lombok.Data;

@Data
public class TrainingResponse {
  private Long id;
  private TrainingType trainingType;
  private int durationInMonths;
  private boolean isActive;

  private Long levelId;
  private String levelCode;
  private String levelLabel;

  private Long specialtyId;
  private String specialtyCode;
  private String specialtyLabel;
}
