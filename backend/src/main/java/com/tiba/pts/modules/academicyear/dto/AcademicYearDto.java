package com.tiba.pts.modules.academicyear.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiba.pts.core.dto.BaseDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AcademicYearDto extends BaseDto {

  @NotBlank(message = "LABEL_REQUIRED")
  private String label;

  private Boolean isActive;

  @NotNull(message = "START_DATE_REQUIRED")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @NotNull(message = "END_DATE_REQUIRED")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  @JsonIgnore
  @AssertTrue(message = "INVALID_DATE_RANGE")
  public boolean isDateRangeValid() {
    if (startDate == null || endDate == null) {
      return true;
    }
    return endDate.isAfter(startDate);
  }
}
