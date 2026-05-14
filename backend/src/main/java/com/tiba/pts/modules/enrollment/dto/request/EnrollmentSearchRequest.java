package com.tiba.pts.modules.enrollment.dto.request;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentSearchRequest {

  private String keyword;
  private Long levelId;
  private Long specialtyId;
  private Long promotionId;
  private EnrollmentStatus status;

  @Builder.Default private int page = 0;

  @Builder.Default private int size = 5;
}
