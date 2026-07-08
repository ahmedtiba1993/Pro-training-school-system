package com.tiba.pts.modules.examscheduling.dto.response;

import com.tiba.pts.modules.examscheduling.domain.enums.ExamTimetableStatus;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamTimetableResponse {
  private Long id;
  private String name;
  private Long classGroupId;
  private String classGroupName;
  private Long periodId;
  private String periodLabel;
  private Long examSessionId;
  private String examSessionLabel;
  private SessionType sessionType;
  private ExamTimetableStatus status;
  private LocalDateTime createdDate;
  private LocalDateTime modifiedDate;
}
