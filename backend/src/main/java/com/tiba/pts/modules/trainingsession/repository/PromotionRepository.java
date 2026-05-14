package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
  boolean existsByCode(String code);

  long countByStatusAndTraining_TrainingType(PromotionStatus status, TrainingType trainingType);

  @Query(
      "SELECT p FROM Promotion p WHERE p.training.id = :trainingId "
          + "AND (p.status = :status OR (CURRENT_DATE >= p.registrationOpeningDate AND CURRENT_DATE <= p.registrationDeadline))")
  List<Promotion> findByTrainingIdAndStatusOrRegistrationOpen(
      @Param("trainingId") Long trainingId, @Param("status") PromotionStatus status);

  boolean existsByTrainingId(Long id);

  boolean existsByCodeIgnoreCase(String generatedCode);

  List<Promotion> findByStatusIn(List<PromotionStatus> statuses);
}
