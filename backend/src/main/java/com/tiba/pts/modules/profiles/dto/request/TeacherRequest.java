package com.tiba.pts.modules.profiles.dto.request;

import com.tiba.pts.modules.profiles.domain.enums.ContractType;
import com.tiba.pts.modules.profiles.domain.enums.AcademicDegree;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherRequest extends PersonRequest {

  @NotNull(message = "CONTRACT_TYPE_REQUIRED")
  private ContractType contractType;

  @NotNull(message = "HIRE_DATE_REQUIRED")
  private LocalDate hireDate;

  @NotNull(message = "ACADEMIC_DEGREE_REQUIRED")
  private AcademicDegree degree;

  @NotNull(message = "SPECIALTIES_REQUIRED")
  private Set<Long> specialtyIds;
}
