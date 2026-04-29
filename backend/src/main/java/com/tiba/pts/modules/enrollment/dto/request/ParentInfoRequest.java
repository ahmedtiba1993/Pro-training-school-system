package com.tiba.pts.modules.enrollment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParentInfoRequest extends PersonInfoRequest {
  @NotBlank(message = "PROFESSION_REQUIRED")
  private String profession;
}
