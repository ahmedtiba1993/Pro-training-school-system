package com.tiba.pts.modules.dashboard.dto.response;

import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatsResponse {
  private long totalCount;
  private long maleCount;
  private long femaleCount;
  private List<EnrollmentResponse> enrollments;
}
