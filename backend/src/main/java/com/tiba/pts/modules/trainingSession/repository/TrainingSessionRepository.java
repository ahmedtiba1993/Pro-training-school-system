package com.tiba.pts.modules.trainingSession.repository;

import com.tiba.pts.modules.trainingSession.domain.entity.TrainingSession;
import com.tiba.pts.modules.trainingSession.domain.enums.TrainingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

  // save
  boolean existsByPromotionNameIgnoreCase(String promotionName);

  boolean existsByAcademicYearIdAndLevelIdAndSpecialtyId(
      Long academicYearId, Long levelId, Long specialtyId);

  // update
  boolean existsByPromotionNameIgnoreCaseAndIdNot(String promotionName, Long id);

  boolean existsByAcademicYearIdAndLevelIdAndSpecialtyIdAndIdNot(
      Long academicYearId, Long levelId, Long specialtyId, Long id);

  List<TrainingSession> findByStatus(TrainingSessionStatus status);

  List<TrainingSession> findByLevelIdAndSpecialtyIdAndRegistrationsOpenTrue(
      Long levelId, Long specialtyId);
}
