package com.tiba.pts.modules.schedule.dto.request;

import com.tiba.pts.modules.schedule.domain.enums.DayOfWeek;
import com.tiba.pts.modules.schedule.domain.enums.Periodicity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimetableSlotRequest {

  @NotNull(message = "Le jour de la semaine est obligatoire")
  private DayOfWeek dayOfWeek;

  @NotNull(message = "La périodicité est obligatoire")
  private Periodicity periodicity;

  @NotNull(message = "L'ID de l'emploi du temps est obligatoire")
  private Long scheduleId;

  @NotNull(message = "L'ID de la matière est obligatoire")
  private Long subjectId;

  @NotNull(message = "L'ID de l'enseignant est obligatoire")
  private Long teacherId;

  @NotNull(message = "L'ID de la salle est obligatoire")
  private Long roomId;

  @NotNull(message = "L'ID de la définition du créneau est obligatoire")
  private Long timeSlotDefinitionId;
}
