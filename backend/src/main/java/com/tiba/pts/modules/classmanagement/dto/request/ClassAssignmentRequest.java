package com.tiba.pts.modules.classmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassAssignmentRequest {

  @NotNull(message = "CLASS_GROUP_ID_REQUIRED")
  private Long classGroupId;

  @NotNull(message = "ENROLLMENT_ID_REQUIRED")
  private Long enrollmentId;
}
