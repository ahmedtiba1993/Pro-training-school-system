package com.tiba.pts.modules.profiles.dto.response;

import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import lombok.Data;

@Data
public class StudentParentResponse {
  private ParentalLink link;
  private boolean isLegalGuardian;
  private ParentResponse parent;
}
