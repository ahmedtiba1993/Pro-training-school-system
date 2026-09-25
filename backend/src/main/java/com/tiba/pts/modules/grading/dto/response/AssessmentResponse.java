package com.tiba.pts.modules.grading.dto.response;

import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponse {
  private Long id;
  private Long promotionSubjectId;
  private String subjectName;
  private String title;
  private AssessmentType assessmentType;
  private Double totalMarks;
  private Integer weightPercentage;
  private AssessmentStatus status;
}
