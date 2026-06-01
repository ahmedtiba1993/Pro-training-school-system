package com.tiba.pts.modules.schedule.dto.response;

import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleResponse {
  private Long id;
  private String label;
  private String classGroupName;
  private String trainingName; // Concatenation: Level.code + Specialty.label
  private ScheduleStatus status;
  private Long classGroupId;
  private Long periodId;
  private String periodLabel;
}
