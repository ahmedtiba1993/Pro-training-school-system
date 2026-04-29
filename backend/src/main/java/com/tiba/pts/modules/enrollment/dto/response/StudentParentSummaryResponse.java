package com.tiba.pts.modules.enrollment.dto.response;

import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import lombok.Data;

@Data
public class StudentParentSummaryResponse {
  private ParentalLink link;
  private boolean isLegalGuardian;
  private ParentSummaryResponse parent;
}
