package com.tiba.pts.modules.examscheduling.repository;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

  boolean existsByExamTimetableIdAndAssessmentId(Long examTimetableId, Long assessmentId);

  boolean existsByExamTimetableClassGroupIdAndExamDateAndExamTimeSlotId(
      Long classGroupId, LocalDate examDate, Long examTimeSlotId);

  boolean existsByExamTimetableClassGroupIdAndExamDateAndExamTimeSlotIdAndIdNot(
      Long classGroupId, LocalDate examDate, Long examTimeSlotId, Long id);

  java.util.List<ExamSchedule> findByExamTimetableId(Long examTimetableId);
}
