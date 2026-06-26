package com.tiba.pts.modules.billing.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.service.PdfGeneratorService;
import com.tiba.pts.modules.billing.domain.enums.ContractStatus;
import com.tiba.pts.modules.billing.domain.enums.PaymentMethod;
import com.tiba.pts.modules.billing.domain.enums.TransactionStatus;
import com.tiba.pts.modules.billing.domain.model.FinancialContract;
import com.tiba.pts.modules.billing.domain.model.PaymentTransaction;
import com.tiba.pts.modules.billing.dto.request.PaymentTransactionRequest;
import com.tiba.pts.modules.billing.dto.response.PaymentTransactionResponse;
import com.tiba.pts.modules.billing.mapper.PaymentTransactionMapper;
import com.tiba.pts.modules.billing.repository.FinancialContractRepository;
import com.tiba.pts.modules.billing.repository.PaymentTransactionRepository;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

  private final PaymentTransactionRepository paymentTransactionRepository;
  private final FinancialContractRepository financialContractRepository;
  private final PaymentTransactionMapper paymentTransactionMapper;
  private final PdfGeneratorService pdfGeneratorService;

  @Transactional
  public Long create(PaymentTransactionRequest request) {

    // Retrieve the financial contract
    FinancialContract contract =
        financialContractRepository
            .findById(request.getFinancialContractId())
            .orElseThrow(() -> new ResourceNotFoundException("FINANCIAL_CONTRACT_NOT_FOUND"));

    // Unique validation rule: Anti-Overpayment
    BigDecimal netTotalToPay = contract.getTotalAmount().subtract(contract.getDiscountAmount());
    BigDecimal remainingAmount = netTotalToPay.subtract(contract.getPaidAmount());

    if (request.getAmount().compareTo(remainingAmount) > 0) {
      throw new BusinessValidationException("PAYMENT_AMOUNT_EXCEEDS_REMAINING_BALANCE");
    }

    // Strict mapping of the request itself
    PaymentTransaction transaction = paymentTransactionMapper.toEntity(request);

    // Manually enrich the transaction entity
    transaction.setFinancialContract(contract);
    transaction.setReceiptNumber(generateReceiptNumber());

    // Initial status calculation rule (Is the money guaranteed?)
    //    Only guaranteed money (CLEARED) is accounted for in the contract's paidAmount;
    //    a PENDING payment (check, bank transfer) does not clear the debt until it is cleared.
    TransactionStatus initialStatus =
        switch (request.getPaymentMethod()) {
          case CASH, CREDIT_CARD -> TransactionStatus.CLEARED;
          default -> TransactionStatus.PENDING;
        };

    transaction.setStatus(initialStatus);

    // Update the financial state: only guaranteed amount increments the paidAmount
    if (initialStatus == TransactionStatus.CLEARED) {
      addClearedAmount(contract, request.getAmount());
    }

    // Save
    return paymentTransactionRepository.save(transaction).getId();
  }

  /**
   * Retrieves all payment transactions for a given financial contract. Ordered by payment date
   * descending (most recent first).
   */
  @Transactional(readOnly = true)
  public List<PaymentTransactionResponse> getTransactionsByContractId(Long contractId) {
    if (!financialContractRepository.existsById(contractId)) {
      throw new ResourceNotFoundException("FINANCIAL_CONTRACT_NOT_FOUND");
    }

    return paymentTransactionRepository
        .findAllByFinancialContractIdOrderByPaymentDateDesc(contractId)
        .stream()
        .map(paymentTransactionMapper::toResponse)
        .toList();
  }

  /**
   * Retrieves all payment transactions with pagination. Ordered by payment date descending (most
   * recent first).
   */
  @Transactional(readOnly = true)
  public PageResponse<PaymentTransactionResponse> getAllTransactionsPaged(
      LocalDate startDate, LocalDate endDate, int page, int size) {
    // Sort by payment date descending (most recent first)
    Pageable pageable = PageRequest.of(page, size, Sort.by("paymentDate").descending());

    // Convert LocalDate to LocalDateTime (inclusive boundaries)
    LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

    Page<PaymentTransaction> pageResult =
        paymentTransactionRepository.findAllByDateRange(startDateTime, endDateTime, pageable);

    return PageResponse.of(pageResult, paymentTransactionMapper::toResponse);
  }

  // ===========================================================================
  // PRINT PAYMENT RECEIPT (PDF)
  // ===========================================================================

  @Transactional(readOnly = true)
  public byte[] generateReceiptPdf(Long transactionId) {
    PaymentTransaction transaction =
        paymentTransactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_TRANSACTION_NOT_FOUND"));

    TransactionStatus status = transaction.getStatus();
    if (status == TransactionStatus.CANCELLED
        || status == TransactionStatus.BOUNCED
        || status == TransactionStatus.REFUNDED) {
      throw new BusinessValidationException("RECEIPT_NOT_AVAILABLE_FOR_TRANSACTION_STATUS");
    }

    FinancialContract contract = transaction.getFinancialContract();
    Enrollment enrollment = contract.getEnrollment();
    Student student = enrollment.getStudent();
    Promotion promotion = enrollment.getPromotion();

    BigDecimal amount = transaction.getAmount();
    BigDecimal totalAmount = contract.getTotalAmount();
    BigDecimal discountAmount = contract.getDiscountAmount();
    BigDecimal netTotal = totalAmount.subtract(discountAmount);

    // paidAmount inclut déjà ce paiement si CLEARED ; on l'exclut pour obtenir le "déjà payé avant".
    BigDecimal previousPaid =
        status == TransactionStatus.CLEARED
            ? contract.getPaidAmount().subtract(amount)
            : contract.getPaidAmount();
    BigDecimal remaining = netTotal.subtract(previousPaid).subtract(amount).max(BigDecimal.ZERO);

    Map<String, Object> variables = new java.util.HashMap<>();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
    currencyFormat.setMinimumFractionDigits(2);
    currencyFormat.setMaximumFractionDigits(2);

    // Transaction information
    variables.put("receiptNumber", transaction.getReceiptNumber());
    variables.put(
        "paymentDate",
        transaction.getPaymentDate() != null
            ? transaction.getPaymentDate().format(dateTimeFormatter)
            : "");
    variables.put("paymentMethod", toFrLabel(transaction.getPaymentMethod()));
    variables.put("status", toFrLabel(status));
    variables.put("reference", transaction.getReference());

    // Student identity
    variables.put("studentName", student.getFirstName() + " " + student.getLastName());
    variables.put("studentMatricule", student.getStudentCode());
    variables.put("contractNumber", contract.getContractNumber());

    // Training / Course details
    variables.put("levelName", promotion.getTraining().getLevel().getLabel());
    variables.put("specialtyName", promotion.getTraining().getSpecialty().getLabel());
    variables.put("promotionName", promotion.getName());

    // Settled amount
    variables.put("amountFormatted", currencyFormat.format(amount) + " TND");
    variables.put("amountInWords", amountToWords(amount));

    // Financial situation
    BigDecimal totalPaid = previousPaid.add(amount);
    variables.put("totalAmountFormatted", currencyFormat.format(totalAmount) + " TND");
    variables.put("discountAmountFormatted", currencyFormat.format(discountAmount) + " TND");
    variables.put("netTotalFormatted", currencyFormat.format(netTotal) + " TND");
    variables.put("previousPaidFormatted", currencyFormat.format(previousPaid) + " TND");
    variables.put("totalPaidFormatted", currencyFormat.format(totalPaid) + " TND");
    variables.put("remainingFormatted", currencyFormat.format(remaining) + " TND");

    variables.put("currentDate", LocalDate.now().format(dateFormatter));

    return pdfGeneratorService.generatePdf("payment-receipt", variables);
  }

  /** Converts a payment method to a readable French label. */
  private String toFrLabel(PaymentMethod method) {
    return switch (method) {
      case CASH -> "Espèces";
      case CHECK -> "Chèque";
      case BANK_TRANSFER -> "Virement Bancaire";
      case CREDIT_CARD -> "Carte Bancaire";
      case ONLINE_STRIPE -> "Paiement en Ligne";
    };
  }

  /** Converts a transaction status to a readable French label. */
  private String toFrLabel(TransactionStatus status) {
    return switch (status) {
      case PENDING -> "En attente";
      case CLEARED -> "Validé";
      case BOUNCED -> "Rejeté";
      case CANCELLED -> "Annulé";
      case REFUNDED -> "Remboursé";
    };
  }

  /**
   * Converts an amount to words (French). E.g.: "1 250.00" -> "mille deux cent cinquante dinars".
   */
  private String amountToWords(BigDecimal amount) {
    long whole = amount.longValue();
    String words = convertLongToFrenchWords(whole);
    return words + " dinars";
  }

  private String convertLongToFrenchWords(long number) {
    if (number == 0) {
      return "zéro";
    }
    String[] units = {
      "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze",
      "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"
    };
    String[] tens = {
      "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre-vingt",
      "quatre-vingt"
    };

    StringBuilder result = new StringBuilder();

    if (number >= 1_000_000) {
      appendCompound(result, number / 1_000_000, units, tens);
      result.append(" million");
      if (number / 1_000_000 > 1) {
        result.append("s");
      }
      number %= 1_000_000;
      if (number > 0) {
        result.append(" ");
      }
    }
    if (number >= 1000) {
      long thousands = number / 1000;
      if (thousands > 1) {
        appendCompound(result, thousands, units, tens);
        result.append(" ");
      }
      result.append("mille");
      number %= 1000;
      if (number > 0) {
        result.append(" ");
      }
    }
    if (number >= 100) {
      long hundreds = number / 100;
      if (hundreds > 1) {
        result.append(units[(int) hundreds]).append(" ");
      }
      result.append("cent");
      if (hundreds > 1 && number % 100 == 0) {
        result.append("s");
      }
      number %= 100;
      if (number > 0) {
        result.append(" ");
      }
    }
    if (number > 0) {
      appendCompound(result, number, units, tens);
    }
    return result.toString().trim();
  }

  /** Append the French words for a value below 100 (0-99). */
  private void appendCompound(StringBuilder result, long number, String[] units, String[] tens) {
    if (number < 20) {
      result.append(units[(int) number]);
    } else {
      int tenIndex = (int) (number / 10);
      int unit = (int) (number % 10);
      if (tenIndex == 7 || tenIndex == 9) {
        // 70-79 → soixante-dix..soixante-dix-neuf ; 90-99 → quatre-vingt-dix..
        result.append(tens[tenIndex]).append("-");
        result.append(units[10 + unit]);
      } else if (tenIndex == 8 && unit == 0) {
        result.append("quatre-vingts");
      } else {
        result.append(tens[tenIndex]);
        if (unit == 1 && tenIndex != 8) {
          result.append(" et un");
        } else if (unit > 0) {
          result.append("-").append(units[unit]);
        }
      }
    }
  }

  /**
   * Generates a unique receipt number with format: REC-YYYY-XXXX (e.g., REC-2024-0001,
   * REC-2024-0002, ..., REC-2024-9999).
   */
  private String generateReceiptNumber() {
    int currentYear = LocalDate.now().getYear();
    String prefix = "REC-" + currentYear + "-";

    Optional<PaymentTransaction> lastTransaction =
        paymentTransactionRepository.findTopByReceiptNumberStartingWithOrderByReceiptNumberDesc(
            prefix);

    int sequence = 1;

    if (lastTransaction.isPresent()) {
      String lastNumber = lastTransaction.get().getReceiptNumber();
      String sequenceStr = lastNumber.substring(prefix.length());
      try {
        sequence = Integer.parseInt(sequenceStr) + 1;
      } catch (NumberFormatException e) {
        sequence = 1;
      }
    }

    return prefix + String.format("%04d", sequence);
  }

  // ===========================================================================
  // TRANSACTION STATUS CHANGE
  // ===========================================================================

  /**
   * Changes the status of a payment transaction and adjusts the linked contract's financial state.
   *
   * <p>Workflow (only guaranteed money — CLEARED — is accounted for in the contract's paidAmount):
   *
   * <ul>
   *   <li><b>PENDING → CLEARED</b>: money becomes guaranteed → we ADD the amount to paidAmount
   *       (and switch the contract to FULLY_PAID if settled). The linked contract must not be CANCELLED.
   *   <li><b>PENDING → BOUNCED</b>: payment rejected before validation → no financial incidence
   *       (never counted in paidAmount).
   *   <li><b>PENDING → CANCELLED</b>: payment cancelled before validation → no financial incidence.
   *   <li><b>CLEARED → BOUNCED</b>: a guaranteed payment is rejected a posteriori → we SUBTRACT the
   *       amount from paidAmount. If the contract was FULLY_PAID (settled), it goes back to ACTIVE
   *       (debt reopened).
   * </ul>
   *
   * @param transactionId ID of the transaction to update
   * @param newStatus new target status
   */
  @Transactional
  public void changeStatus(Long transactionId, TransactionStatus newStatus) {
    PaymentTransaction transaction =
        paymentTransactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_TRANSACTION_NOT_FOUND"));

    TransactionStatus currentStatus = transaction.getStatus();
    FinancialContract contract = transaction.getFinancialContract();

    // 1. Validation of the authorized transition
    validateTransition(currentStatus, newStatus, contract);

    // 2. Financial adjustment of the contract according to the transition
    BigDecimal amount = transaction.getAmount();
    switch (newStatus) {
      case CLEARED -> {
        // PENDING → CLEARED: money becomes guaranteed, we account for it
        addClearedAmount(contract, amount);
      }
      case BOUNCED, CANCELLED -> {
        // Money deduction only if the payment was already cleared (CLEARED)
        if (currentStatus == TransactionStatus.CLEARED) {
          revertClearedAmount(contract, amount);
        }
        // PENDING → BOUNCED / CANCELLED: no incidence (never accounted for)
      }
      default -> throw new BusinessValidationException("INVALID_TRANSACTION_STATUS_TRANSITION");
    }

    transaction.setStatus(newStatus);

    paymentTransactionRepository.save(transaction);
    financialContractRepository.save(contract);
  }

  /**
   * Verifies that a status transition is authorized according to the defined workflow and that
   * the linked contract allows the operation.
   */
  private void validateTransition(
      TransactionStatus currentStatus, TransactionStatus newStatus, FinancialContract contract) {
    boolean allowed =
        switch (currentStatus) {
          case PENDING -> newStatus == TransactionStatus.CLEARED
              || newStatus == TransactionStatus.BOUNCED
              || newStatus == TransactionStatus.CANCELLED;
          case CLEARED -> newStatus == TransactionStatus.BOUNCED;
          default -> false; // BOUNCED, CANCELLED, REFUNDED = terminal states
        };

    if (!allowed) {
      throw new BusinessValidationException(
          "INVALID_TRANSACTION_STATUS_TRANSITION");
    }

    // The linked contract must not be CANCELLED to clear/add money
    if (contract.getStatus() == ContractStatus.CANCELLED
        && newStatus == TransactionStatus.CLEARED) {
      throw new BusinessValidationException("CONTRACT_IS_CANCELLED");
    }
  }

  /**
   * Adds a guaranteed amount to the contract's paidAmount and changes it to FULLY_PAID if the net to
   * pay is reached. (Used during CASH/CREDIT_CARD creation and during a PENDING -> CLEARED transition.)
   */
  private void addClearedAmount(FinancialContract contract, BigDecimal amount) {
    BigDecimal netTotalToPay = contract.getTotalAmount().subtract(contract.getDiscountAmount());
    BigDecimal newPaidAmount = contract.getPaidAmount().add(amount);
    contract.setPaidAmount(newPaidAmount);

    if (newPaidAmount.compareTo(netTotalToPay) == 0) {
      contract.setStatus(ContractStatus.FULLY_PAID);
    }
  }

  /**
   * Subtracts a guaranteed amount from the contract's paidAmount (confirmed payment rejected a posteriori).
   * If the contract was FULLY_PAID (settled), the subtraction automatically reverts it to ACTIVE
   * (debt reopened).
   */
  private void revertClearedAmount(FinancialContract contract, BigDecimal amount) {
    BigDecimal newPaidAmount = contract.getPaidAmount().subtract(amount);
    contract.setPaidAmount(newPaidAmount);

    if (contract.getStatus() == ContractStatus.FULLY_PAID) {
      contract.setStatus(ContractStatus.ACTIVE);
    }
  }
}
