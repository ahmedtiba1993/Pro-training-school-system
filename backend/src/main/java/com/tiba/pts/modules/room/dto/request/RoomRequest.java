package com.tiba.pts.modules.room.dto.request;

import com.tiba.pts.modules.room.domain.enums.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
    @NotBlank(message = "NAME_REQUIRED") String name,
    @NotBlank(message = "EMPLACEMENT_REQUIRED") String emplacement,
    @NotNull(message = "CAPACITY_REQUIRED")
        @Min(value = 1, message = "CAPACITY_MUST_BE_GREATER_THAN_ZERO")
        Integer capacity,
    @NotNull(message = "TYPE_REQUIRED") RoomType type) {
  // Compact constructor: automatically removes spaces in the request
  public RoomRequest {
    name = name != null ? name.trim() : null;
    emplacement = emplacement != null ? emplacement.trim() : null;
  }
}
