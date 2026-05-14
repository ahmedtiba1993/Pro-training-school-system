package com.tiba.pts.modules.enrollment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentParentInfoRequest {

  @NotNull(message = "PARENTAL_LINK_REQUIRED")
  private ParentalLink link;

  @JsonProperty("isLegalGuardian")
  private boolean isLegalGuardian;

  // The ID of the parent if they already exist in the database
  private Long existingParentId;

  // The parent's information if they are new (no more @NotNull here)
  @Valid private ParentInfoRequest parent;

  @JsonIgnore
  @AssertTrue(message = "YOU_MUST_PROVIDE_EITHER_EXISTING_PARENT_ID_OR_NEW_PARENT_INFO")
  public boolean isParentDataProvided() {
    boolean hasExistingId = (existingParentId != null);
    boolean hasNewParent = (parent != null);

    // XOR: Returns true IF AND ONLY IF one of the two is provided, but not both at the same time
    return hasExistingId ^ hasNewParent;
  }
}
