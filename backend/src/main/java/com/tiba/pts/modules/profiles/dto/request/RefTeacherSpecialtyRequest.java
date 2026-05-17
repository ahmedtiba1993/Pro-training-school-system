package com.tiba.pts.modules.profiles.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefTeacherSpecialtyRequest {

  @NotBlank(message = "SPECIALTY_CODE_REQUIRED")
  private String code;

  @NotBlank(message = "SPECIALTY_LABEL_REQUIRED")
  private String label;

  private String description;

  // Jackson will call this setter upon receiving the JSON
  public void setCode(String code) {
    this.code = (code != null) ? code.trim().toUpperCase() : null;
  }

  // Jackson will call this setter upon receiving the JSON
  public void setLabel(String label) {
    this.label = (label != null) ? label.trim() : null;
  }

  public void setDescription(String description) {
    this.description = (description != null) ? description.trim() : null;
  }
}
