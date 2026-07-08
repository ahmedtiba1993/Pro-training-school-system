package com.tiba.pts.modules.grading.dto.response;

import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentLookupResponse {
  private Long id;
  private String subjectName;
  private AssessmentType assessmentType;
}
