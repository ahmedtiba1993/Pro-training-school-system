package com.tiba.pts.modules.examscheduling.repository;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamTimetableRepository extends JpaRepository<ExamTimetable, Long> {

  boolean existsByClassGroupIdAndPeriodIdAndExamSessionSessionType(
      Long classGroupId, Long periodId, SessionType sessionType);

  boolean existsByClassGroupIdAndExamSessionId(Long classGroupId, Long examSessionId);
}
