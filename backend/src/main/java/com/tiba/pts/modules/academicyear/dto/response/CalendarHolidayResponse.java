package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.HolidayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarHolidayResponse {

  private Long id;
  private String label;
  private HolidayType type;
  private LocalDate startDate;
  private LocalDate endDate;
  private Long numberOfDays;
}
