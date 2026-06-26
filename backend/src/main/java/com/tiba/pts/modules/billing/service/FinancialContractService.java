package com.tiba.pts.modules.billing.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;

import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.billing.domain.enums.ContractStatus;
import com.tiba.pts.modules.billing.domain.enums.PaymentMethod;
import com.tiba.pts.modules.billing.domain.model.FinancialContract;
import com.tiba.pts.modules.billing.dto.request.FinancialContractSearchRequest;
import com.tiba.pts.modules.billing.dto.request.PaymentTransactionRequest;
import com.tiba.pts.modules.billing.dto.response.ActiveRevenueStatResponse;
import com.tiba.pts.modules.billing.dto.response.FinancialContractListResponse;
import com.tiba.pts.modules.billing.mapper.FinancialContractMapper;
import com.tiba.pts.modules.billing.repository.FinancialContractRepository;
import com.tiba.pts.modules.billing.repository.projection.FinancialStatProjection;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FinancialContractService {

  private final FinancialContractRepository financialContractRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final FinancialContractMapper financialContractMapper;
  private final PaymentTransactionService paymentTransactionService;

  /**
   * Creates a FinancialContract for the given enrollment. This is an internal method (no API
   * endpoint), called by other services.
   */
  @Transactional
  public Long createContract(Enrollment enrollment) {

    // Verify no contract already exists for this enrollment (strict 1-to-1)
    if (financialContractRepository.existsByEnrollmentId(enrollment.getId())) {
      return null;
    }

    // Read the fees from the Promotion linked to the enrollment
    BigDecimal registrationFee = enrollment.getPromotion().getRegistrationFee();
    BigDecimal tuitionFee = enrollment.getPromotion().getTuitionFee();

    // Calculate total amount
    BigDecimal totalAmount = registrationFee.add(tuitionFee);

    // Generate the contract number (FACT-YYYY-XXX)
    String contractNumber = generateContractNumber();

    // Map to FinancialContract entity (status DRAFT, discountAmount = 0, paidAmount = 0)
    FinancialContract contract =
        financialContractMapper.toEntity(contractNumber, enrollment, totalAmount);

    // Persist and return
    return financialContractRepository.save(contract).getId();
  }

  /**
   * Generates a unique contract number with format: FACT-YYYY-XXX (e.g., FACT-2026-001,
   * FACT-2024-002, ..., FACT-2024-999).
   */
  private String generateContractNumber() {
    int currentYear = LocalDate.now().getYear();
    String prefix = "FACT-" + currentYear + "-";

    Optional<FinancialContract> lastContract =
        financialContractRepository.findTopByContractNumberStartingWithOrderByContractNumberDesc(
            prefix);

    int sequence = 1;

    if (lastContract.isPresent()) {
      String lastNumber = lastContract.get().getContractNumber();
      String sequenceStr = lastNumber.substring(prefix.length());
      try {
        sequence = Integer.parseInt(sequenceStr) + 1;
      } catch (NumberFormatException e) {
        sequence = 1;
      }
    }

    return prefix + String.format("%03d", sequence);
  }

  @Transactional(readOnly = true)
  public PageResponse<FinancialContractListResponse> getAllContractsPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<FinancialContract> pageResult =
        financialContractRepository.findAllPagedWithDetails(pageable);
    return PageResponse.of(pageResult, financialContractMapper::toListResponse);
  }

  @Transactional(readOnly = true)
  public PageResponse<FinancialContractListResponse> getAllContractsPaged(
      FinancialContractSearchRequest searchParams) {

    Pageable pageable =
        PageRequest.of(
            searchParams.getPage(), searchParams.getSize(), Sort.by("createdDate").descending());

    Page<FinancialContract> pageResult =
        financialContractRepository.findAllWithFilters(
            searchParams.getKeyword(),
            searchParams.getStatus(),
            searchParams.getPromotionId(),
            searchParams.getTrainingType(),
            pageable);

    return PageResponse.of(pageResult, financialContractMapper::toListResponse);
  }

  /**
   * Retrieves aggregated active revenue statistics.
   */
  @Transactional(readOnly = true)
  public ActiveRevenueStatResponse getActiveRevenueStat(TrainingType trainingType) {
    if (trainingType == null) {
      BigDecimal globalDebt = financialContractRepository.calculateGlobalRemainingDebt();
      return ActiveRevenueStatResponse.builder()
          .remainingDebt(globalDebt)
          .build();
    }

    // aggregated metrics via Spring Data projection
    FinancialStatProjection stats =
        switch (trainingType) {
          case ACCREDITED -> financialContractRepository.getAccreditedStats(trainingType);
          case ACCELERATED, CONTINUOUS ->
              financialContractRepository.getOngoingStats(trainingType);
        };

    // Pending payments (separate entity: PaymentTransaction)
    BigDecimal pendingPayments =
        financialContractRepository.calculateTotalPendingTransactions(trainingType);

    // Build response
    return ActiveRevenueStatResponse.builder()
        .activeRevenue(stats.getActiveRevenue())
        .totalCollected(stats.getTotalCollected())
        .remainingDebt(stats.getRemainingDebt())
        .losses(stats.getLosses())
        .pendingPayments(pendingPayments)
        .build();
  }

  @Transactional
  public void activateContract(
      Long id,
      BigDecimal discountAmount,
      PaymentMethod paymentMethod,
      String reference,
      LocalDateTime paymentDate) {
    FinancialContract contract =
        financialContractRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Financial contract not found with ID: " + id));

    if (contract.getStatus() != ContractStatus.DRAFT) {
      throw new BusinessValidationException("Only DRAFT contracts can be activated.");
    }

    if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessValidationException("Discount amount cannot be negative.");
    }

    if (discountAmount.compareTo(contract.getTotalAmount()) > 0) {
      throw new BusinessValidationException(
          "Discount amount cannot be greater than the total contract amount.");
    }

    // Update the status and discount of the contract
    contract.setDiscountAmount(discountAmount);
    contract.setStatus(ContractStatus.ACTIVE);

    financialContractRepository.save(contract);

    // Retrieve registration fee amount from the promotion
    if (contract.getEnrollment() == null
        || contract.getEnrollment().getPromotion() == null
        || contract.getEnrollment().getPromotion().getRegistrationFee() == null) {
      throw new BusinessValidationException("REGISTRATION_FEE_NOT_DEFINED_FOR_PROMOTION");
    }

    BigDecimal registrationFee = contract.getEnrollment().getPromotion().getRegistrationFee();

    if (registrationFee.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessValidationException("REGISTRATION_FEE_MUST_BE_POSITIVE");
    }

    // Record registration fee payment as the first transaction by calling the create method
    PaymentTransactionRequest transactionRequest =
        PaymentTransactionRequest.builder()
            .financialContractId(id)
            .amount(registrationFee)
            .paymentMethod(paymentMethod)
            .paymentDate(paymentDate != null ? paymentDate : LocalDateTime.now())
            .reference(reference)
            .build();

    paymentTransactionService.create(transactionRequest);
  }

}
