package com.tiba.pts.modules.grading.repository;

import com.tiba.pts.modules.grading.domain.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

  @Query("SELECT a FROM Assessment a " +
         "JOIN a.promotionSubject ps " +
         "WHERE ps.promotion.id = (SELECT cg.promotion.id FROM ClassGroup cg WHERE cg.id = :classId) " +
         "AND a.assessmentType IN ('FINAL_EXAM', 'RETAKE') " +
         "AND a.id NOT IN (SELECT es.assessment.id FROM ExamSchedule es WHERE es.examTimetable.id = :timetableId)")
  List<Assessment> findUnscheduledAssessmentsForTimetable(@Param("classId") Long classId, @Param("timetableId") Long timetableId);

  @Query("SELECT COALESCE(SUM(a.weightPercentage), 0) FROM Assessment a WHERE a.promotionSubject.id = :promotionSubjectId AND a.assessmentType != 'RETAKE'")
  Integer sumWeightPercentageByPromotionSubjectId(@Param("promotionSubjectId") Long promotionSubjectId);
}
