package com.tiba.pts.modules.grading.repository;

import com.tiba.pts.modules.grading.domain.entity.GradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRecordRepository extends JpaRepository<GradeRecord, Long> {
  Optional<GradeRecord> findByAssessmentIdAndEnrollmentId(Long assessmentId, Long enrollmentId);

  @Query("""
      SELECT gr FROM GradeRecord gr
      JOIN FETCH gr.enrollment e
      JOIN FETCH e.student s
      WHERE gr.assessment.id = :assessmentId
      ORDER BY s.lastName ASC, s.firstName ASC
  """)
  List<GradeRecord> findByAssessmentIdWithStudent(@Param("assessmentId") Long assessmentId);
}
