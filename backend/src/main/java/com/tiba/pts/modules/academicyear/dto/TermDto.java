package com.tiba.pts.modules.academicyear.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tiba.pts.core.dto.BaseDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TermDto extends BaseDto {

  @NotBlank(message = "TERM_NAME_REQUIRED")
  private String name;

  @NotNull(message = "START_DATE_REQUIRED")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @NotNull(message = "END_DATE_REQUIRED")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  @NotNull(message = "ACADEMIC_YEAR_ID_REQUIRED")
  private Long academicYearId;

  private List<ExamSessionDto> examSessions;

  @JsonIgnore
  @AssertTrue(message = "INVALID_DATE_RANGE")
  public boolean isDateRangeValid() {
    if (startDate == null || endDate == null) {
      return true;
    }
    return endDate.isAfter(startDate);
  }
}
