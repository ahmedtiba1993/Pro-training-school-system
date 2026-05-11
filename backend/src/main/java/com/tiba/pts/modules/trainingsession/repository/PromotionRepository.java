package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
  boolean existsByCode(String code);

  long countByStatusAndTraining_TrainingType(PromotionStatus status, TrainingType trainingType);

  List<Promotion> findByTrainingIdAndStatus(Long trainingId, PromotionStatus status);

  boolean existsByTrainingId(Long id);

  boolean existsByCodeIgnoreCase(String generatedCode);
}
