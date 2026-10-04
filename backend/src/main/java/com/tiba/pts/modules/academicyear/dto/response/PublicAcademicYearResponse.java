package com.tiba.pts.modules.academicyear.dto.response;

import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicAcademicYearResponse {

  private Long id;
  private String label;
  private LocalDate startDate;
  private LocalDate endDate;
  private YearStatus status;
  private Boolean isDefault;
  private Boolean isLocked;
}
