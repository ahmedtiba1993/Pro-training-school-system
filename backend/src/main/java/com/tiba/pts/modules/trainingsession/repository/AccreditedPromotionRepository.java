package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.repository.projection.PromotionStatusStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccreditedPromotionRepository extends JpaRepository<AccreditedPromotion, Long> {
  // To retrieve with a limit
  Page<AccreditedPromotion> findByStatus(PromotionStatus status, Pageable pageable);

  // To retrieve all of a status
  List<AccreditedPromotion> findByStatus(PromotionStatus status, Sort sort);

  boolean existsByTrainingIdAndAcademicYearId(Long trainingId, Long academicYearId);

  @Modifying
  @Query(
      """
        UPDATE AccreditedPromotion p
        SET p.status = :newStatus
        WHERE p.academicYear.id = :yearId
        AND p.status != 'CANCELLED'
        AND p.status != 'COMPLETED'
    """)
  void updateStatusByAcademicYearId(
      @Param("newStatus") PromotionStatus newStatus, @Param("yearId") Long yearId);

  @Query(
      """
      SELECT p.status AS status,
             COUNT(p) AS promotionCount,
             COALESCE(SUM(p.enrollmentCount), 0) AS totalEnrollments
      FROM AccreditedPromotion p
      WHERE p.status IN :statuses
      GROUP BY p.status
  """)
  List<PromotionStatusStatsProjection> getStatsByStatuses(
      @Param("statuses") List<PromotionStatus> statuses);

  List<AccreditedPromotion> findByStatusIn(List<PromotionStatus> statuses, Sort sort);

  Page<AccreditedPromotion> findByStatusIn(List<PromotionStatus> statuses, Pageable pageable);
}
