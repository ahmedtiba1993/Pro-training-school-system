package com.tiba.pts.modules.examscheduling.dto.response;

import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamScheduleResponse {
  private Long id;
  private Long timetableId;
  private Long assessmentId;
  private AssessmentType assessmentType;
  private LocalDate examDate;
  private Long examTimeSlotId;
  private String examTimeSlotLabel;
  private String subjectName;
}
