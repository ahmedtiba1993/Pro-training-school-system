package com.tiba.pts.modules.enrollment.dto.response;

import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentSimpleResponse {
  private Long id;
  private String enrollmentNumber;
  private EnrollmentType type;
  private EnrollmentStatus status;
  private PromotionSummaryResponse promotion;
}
