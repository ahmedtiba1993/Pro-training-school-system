package com.tiba.pts.modules.profiles.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExistenceCheckResponse {
  private boolean cinExists;
  private boolean emailExists;
  private boolean phoneExists;

  // Small utility method to check if at least one of the fields exists
  public boolean hasAnyConflict() {
    return cinExists || emailExists || phoneExists;
  }
}
