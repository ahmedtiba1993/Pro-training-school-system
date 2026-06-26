package com.tiba.pts.modules.billing.repository;

import com.tiba.pts.modules.billing.domain.enums.ContractStatus;
import com.tiba.pts.modules.billing.domain.model.FinancialContract;
import com.tiba.pts.modules.billing.repository.projection.FinancialStatProjection;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface FinancialContractRepository extends JpaRepository<FinancialContract, Long> {

  boolean existsByEnrollmentId(Long enrollmentId);

  Optional<FinancialContract> findTopByContractNumberStartingWithOrderByContractNumberDesc(
      String prefix);

  @Query(
      value =
          "SELECT c FROM FinancialContract c "
              + "JOIN FETCH c.enrollment e "
              + "JOIN FETCH e.student s",
      countQuery = "SELECT count(c) FROM FinancialContract c")
  Page<FinancialContract> findAllPagedWithDetails(Pageable pageable);

  @Query(
      value =
          "SELECT c FROM FinancialContract c "
              + "JOIN FETCH c.enrollment e "
              + "JOIN FETCH e.student s "
              + "JOIN FETCH e.promotion p "
              + "JOIN FETCH p.training t "
              + "WHERE (:keyword IS NULL OR "
              + "       LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.cin) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
              + "AND (:status IS NULL OR c.status = :status) "
              + "AND (:promotionId IS NULL OR p.id = :promotionId) "
              + "AND (:trainingType IS NULL OR t.trainingType = :trainingType)",
      countQuery =
          "SELECT count(c) FROM FinancialContract c "
              + "JOIN c.enrollment e "
              + "JOIN e.student s "
              + "JOIN e.promotion p "
              + "JOIN p.training t "
              + "WHERE (:keyword IS NULL OR "
              + "       LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.cin) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
              + "       LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
              + "AND (:status IS NULL OR c.status = :status) "
              + "AND (:promotionId IS NULL OR p.id = :promotionId) "
              + "AND (:trainingType IS NULL OR t.trainingType = :trainingType)")
  Page<FinancialContract> findAllWithFilters(
      @Param("keyword") String keyword,
      @Param("status") ContractStatus status,
      @Param("promotionId") Long promotionId,
      @Param("trainingType") TrainingType trainingType,
      Pageable pageable);

  // UNIFIED FINANCIAL STATISTICS

  /**
   * Aggregated statistics for Accredited promotions (AccreditedPromotion)
   * of the default academic year (isDefault = true).
   */
  @Query("""
      SELECT
        COALESCE(SUM(CASE
          WHEN fc.status IN ('ACTIVE', 'FULLY_PAID') THEN (fc.totalAmount - fc.discountAmount)
          WHEN fc.status = 'SETTLED_WITH_DEBT' THEN fc.paidAmount
          ELSE 0 END), 0) AS activeRevenue,
        COALESCE(SUM(fc.paidAmount), 0) AS totalCollected,
        COALESCE(SUM(CASE
          WHEN fc.status = 'ACTIVE' THEN (fc.totalAmount - fc.discountAmount - fc.paidAmount)
          ELSE 0 END), 0) AS remainingDebt,
        COALESCE(SUM(CASE
          WHEN fc.status = 'SETTLED_WITH_DEBT' THEN ((fc.totalAmount - fc.discountAmount) - fc.paidAmount)
          ELSE 0 END), 0) AS losses
      FROM FinancialContract fc
        JOIN fc.enrollment e
        JOIN e.promotion p
        JOIN p.training t
      WHERE TYPE(p) = AccreditedPromotion
        AND p.academicYear.isDefault = true
        AND (:trainingType IS NULL OR t.trainingType = :trainingType)
      """)
  FinancialStatProjection getAccreditedStats(@Param("trainingType") TrainingType trainingType);

  /**
   * Aggregated statistics for ongoing promotions (ACCELERATED / CONTINUOUS)
   * filtered by active pedagogical statuses (ENROLLMENT, IN_PROGRESS, EVALUATION).
   * Returns 4 metrics in a single DB round-trip via Spring Data projection.
   */
  @Query("""
      SELECT
        COALESCE(SUM(CASE
          WHEN fc.status IN ('ACTIVE', 'FULLY_PAID') THEN (fc.totalAmount - fc.discountAmount)
          WHEN fc.status = 'SETTLED_WITH_DEBT' THEN fc.paidAmount
          ELSE 0 END), 0) AS activeRevenue,
        COALESCE(SUM(fc.paidAmount), 0) AS totalCollected,
        COALESCE(SUM(CASE
          WHEN fc.status = 'ACTIVE' THEN (fc.totalAmount - fc.discountAmount - fc.paidAmount)
          ELSE 0 END), 0) AS remainingDebt,
        COALESCE(SUM(CASE
          WHEN fc.status = 'SETTLED_WITH_DEBT' THEN ((fc.totalAmount - fc.discountAmount) - fc.paidAmount)
          ELSE 0 END), 0) AS losses
      FROM FinancialContract fc
        JOIN fc.enrollment e
        JOIN e.promotion p
        JOIN p.training t
      WHERE t.trainingType = :type
        AND p.status IN ('ENROLLMENT', 'IN_PROGRESS', 'EVALUATION')
      """)
  FinancialStatProjection getOngoingStats(@Param("type") TrainingType type);

  /**
   * Pending payments (cheques/transfers awaiting bank validation).
   * Kept separate since it queries PaymentTransaction, not FinancialContract.
   */
  @Query("""
      SELECT COALESCE(SUM(pt.amount), 0)
      FROM PaymentTransaction pt
        JOIN pt.financialContract fc
        JOIN fc.enrollment e
        JOIN e.promotion p
        JOIN p.training t
      WHERE pt.status = 'PENDING'
        AND fc.status IN ('ACTIVE', 'FULLY_PAID')
        AND (:trainingType IS NULL OR t.trainingType = :trainingType)
      """)
  BigDecimal calculateTotalPendingTransactions(@Param("trainingType") TrainingType trainingType);

  /**
   * Global remaining debt across all ACTIVE contracts.
   * Optionally filtered by training type (null = all types).
   */
  @Query("""
      SELECT COALESCE(SUM(fc.totalAmount - fc.discountAmount - fc.paidAmount), 0)
      FROM FinancialContract fc
      WHERE fc.status = 'ACTIVE'
      """)
  BigDecimal calculateGlobalRemainingDebt();
}
