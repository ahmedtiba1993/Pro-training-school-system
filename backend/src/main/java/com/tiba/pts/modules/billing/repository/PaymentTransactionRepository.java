package com.tiba.pts.modules.billing.repository;

import com.tiba.pts.modules.billing.domain.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

  Optional<PaymentTransaction> findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(
      String prefix);

  List<PaymentTransaction> findAllByFinancialContractIdOrderByPaymentDateDesc(Long contractId);

  @Query(
      "SELECT t FROM PaymentTransaction t "
          + "WHERE (:startDate IS NULL OR t.paymentDate >= :startDate) "
          + "AND (:endDate IS NULL OR t.paymentDate <= :endDate)")
  Page<PaymentTransaction> findAllByDateRange(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);
}
