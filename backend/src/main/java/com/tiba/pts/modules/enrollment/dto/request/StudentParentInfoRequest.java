package com.tiba.pts.modules.enrollment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentParentInfoRequest {
  @NotNull(message = "PARENTAL_LINK_REQUIRED")
  private ParentalLink link;

  @JsonProperty("isLegalGuardian")
  private boolean isLegalGuardian;

  @Valid
  @NotNull(message = "PARENT_INFO_REQUIRED")
  private ParentInfoRequest parent;
}
