package com.tiba.pts.modules.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleRequest {

  @NotBlank(message = "LABEL_REQUIRED")
  private String label;

  @NotNull(message = "CLASS_GROUP_ID_REQUIRED")
  private Long classGroupId;

  private Long periodId;

  /** Lombok setter override to apply the systematic trim() required by the guidelines */
  public void setLabel(String label) {
    this.label = label != null ? label.trim() : null;
  }
}
