package com.tiba.pts.modules.subject.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubjectRequest {

  @NotBlank(message = "CODE_REQUIRED")
  private String code;

  @NotBlank(message = "NAME_REQUIRED")
  private String name;

  private String description;

  @NotNull(message = "THEORY_HOURS_REQUIRED")
  @Min(value = 0, message = "THEORY_HOURS_INVALID")
  private Integer theoryHours;

  @NotNull(message = "PRACTICAL_HOURS_REQUIRED")
  @Min(value = 0, message = "PRACTICAL_HOURS_INVALID")
  private Integer practicalHours;

  private Double defaultCoefficient;

  private String pdfFilePath;

  @NotNull(message = "TRAINING_ID_REQUIRED")
  private Long trainingId;

  // --- Overloading setters for formatting ---

  public void setCode(String code) {
    this.code = (code != null) ? code.trim().toUpperCase() : null;
  }

  public void setName(String name) {
    this.name = (name != null) ? name.trim() : null;
  }
}
