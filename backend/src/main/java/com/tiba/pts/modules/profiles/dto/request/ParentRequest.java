package com.tiba.pts.modules.profiles.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParentRequest extends PersonRequest {

  @NotBlank(message = "PROFESSION_REQUIRED")
  private String profession;
}
