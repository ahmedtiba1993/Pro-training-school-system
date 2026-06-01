package com.tiba.pts.modules.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleUpdateRequest {

  @NotBlank(message = "LABEL_REQUIRED")
  private String label;

  /** Systematic cleaning of spaces to guarantee the integrity of the label in the database. */
  public void setLabel(String label) {
    this.label = label != null ? label.trim() : null;
  }
}
