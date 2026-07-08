package com.tiba.pts.modules.grading.repository;

import com.tiba.pts.modules.grading.domain.entity.GradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GradeRecordRepository extends JpaRepository<GradeRecord, Long> {
  Optional<GradeRecord> findByAssessmentIdAndEnrollmentId(Long assessmentId, Long enrollmentId);
}
