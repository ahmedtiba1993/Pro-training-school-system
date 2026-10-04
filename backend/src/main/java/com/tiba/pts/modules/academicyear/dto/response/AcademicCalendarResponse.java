package com.tiba.pts.modules.academicyear.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendarResponse {

  private Long id;
  private String label;
  private LocalDate startDate;
  private LocalDate endDate;
  private List<CalendarPeriodResponse> periods;
  private List<CalendarHolidayResponse> holidays;
}
