package com.tiba.pts.modules.academicyear.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.core.dto.BaseDto;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExamSessionDto extends BaseDto {

  @NotNull(message = "SESSION_TYPE_REQUIRED")
  private SessionType sessionType;

  @NotNull(message = "START_DATE_REQUIRED")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @NotNull(message = "END_DATE_REQUIRED")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  @NotNull(message = "TERM_ID_REQUIRED")
  private Long termId;

  @JsonIgnore
  @AssertTrue(message = "INVALID_DATE_RANGE")
  public boolean isDateRangeValid() {
    if (startDate == null || endDate == null) return true;
    return endDate.isAfter(startDate);
  }
}
