package com.tiba.pts.modules.profiles.dto.request;

import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentParentRequest {

  @NotNull(message = "PARENTAL_LINK_REQUIRED")
  private ParentalLink link;

  private boolean isLegalGuardian;

  @Valid
  @NotNull(message = "PARENT_INFO_REQUIRED")
  private ParentRequest parent;
}
