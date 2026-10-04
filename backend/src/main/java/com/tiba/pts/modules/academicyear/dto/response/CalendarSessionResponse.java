package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarSessionResponse {

  private Long id;
  private String label;
  private SessionType sessionType;
  private LocalDate startDate;
  private LocalDate endDate;
}
