package com.tiba.pts.modules.profiles.dto.request;

import com.tiba.pts.modules.profiles.domain.enums.ContractType;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import lombok.Data;

@Data
public class TeacherFiltreRequest {
  private String keyword;
  private Long specialtyId;
  private ContractType contractType;
  private TeacherStatus status;
}
