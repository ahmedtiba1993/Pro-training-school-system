package com.tiba.pts.modules.grading.repository;

import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

  @Query("SELECT a FROM Assessment a JOIN FETCH a.promotionSubject ps JOIN FETCH ps.subject s")
  List<Assessment> findAllWithDetails();

  @EntityGraph(attributePaths = {"promotionSubject", "promotionSubject.subject"})
  @Query("SELECT a FROM Assessment a")
  Page<Assessment> findAllWithDetails(Pageable pageable);

  @Query("SELECT a FROM Assessment a " +
         "JOIN a.promotionSubject ps " +
         "WHERE ps.promotion.id = (SELECT cg.promotion.id FROM ClassGroup cg WHERE cg.id = :classId) " +
         "AND a.assessmentType IN ('FINAL_EXAM', 'RETAKE') " +
         "AND a.id NOT IN (SELECT es.assessment.id FROM ExamSchedule es WHERE es.examTimetable.id = :timetableId)")
  List<Assessment> findUnscheduledAssessmentsForTimetable(@Param("classId") Long classId, @Param("timetableId") Long timetableId);

  @Query("SELECT COALESCE(SUM(a.weightPercentage), 0) FROM Assessment a WHERE a.promotionSubject.id = :promotionSubjectId AND a.assessmentType != 'RETAKE' AND a.status != 'CANCELLED'")
  Integer sumWeightPercentageByPromotionSubjectId(@Param("promotionSubjectId") Long promotionSubjectId);

  List<Assessment> findByPromotionSubjectId(Long promotionSubjectId);

  List<Assessment> findByPromotionSubjectIdAndStatus(Long promotionSubjectId, AssessmentStatus status);

  boolean existsByPromotionSubjectIdAndAssessmentType(Long promotionSubjectId, AssessmentType assessmentType);

  boolean existsByPromotionSubjectIdAndAssessmentTypeAndStatusNot(
      Long promotionSubjectId, AssessmentType assessmentType, AssessmentStatus status);

  boolean existsByPromotionSubjectIdAndAssessmentTypeAndIdNot(
      Long promotionSubjectId, AssessmentType assessmentType, Long id);

  boolean existsByPromotionSubjectIdAndAssessmentTypeAndStatusNotAndIdNot(
      Long promotionSubjectId, AssessmentType assessmentType, AssessmentStatus status, Long id);

  boolean existsByPromotionSubjectIdAndTitleIgnoreCase(Long promotionSubjectId, String title);

  boolean existsByPromotionSubjectIdAndTitleIgnoreCaseAndIdNot(
      Long promotionSubjectId, String title, Long id);
}
