package com.tiba.pts.modules.schedule.dto.request;

import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleStatusRequest {

  @NotNull(message = "Le nouveau statut est obligatoire")
  private ScheduleStatus newStatus;
}
