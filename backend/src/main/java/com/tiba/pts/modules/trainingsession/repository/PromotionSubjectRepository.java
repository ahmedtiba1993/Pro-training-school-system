package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionSubjectRepository extends JpaRepository<PromotionSubject, Long> {

  /**
   * Checks if a subject is already assigned to a promotion for a specific
   * academic period. (Used for uniqueness validation per semester of AccreditedPromotion).
   */
  boolean existsByPromotionIdAndSubjectIdAndAcademicPeriodId(
      Long promotionId, Long subjectId, Long academicPeriodId);

  /**
   * Checks if a subject is already assigned to a promotion, regardless of the period.
   * (Used for absolute uniqueness validation of AcceleratedPromotion and ContinuousPromotion).
   */
  boolean existsByPromotionIdAndSubjectId(Long promotionId, Long subjectId);

  /**
   * Retrieves the list of all subjects assigned to a specific promotion. Useful for the
   * "list of subjects for each promotion" part.
   */
  List<PromotionSubject> findByPromotionId(Long promotionId);

  List<PromotionSubject> findByPromotionIdAndAcademicPeriodId(
      Long promotionId, Long academicPeriodId);
}
