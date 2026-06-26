package com.tiba.pts.modules.billing.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.billing.domain.enums.TransactionStatus;
import com.tiba.pts.modules.billing.dto.request.PaymentTransactionRequest;
import com.tiba.pts.modules.billing.dto.response.PaymentTransactionResponse;
import com.tiba.pts.modules.billing.service.PaymentTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-transactions")
@RequiredArgsConstructor
public class PaymentTransactionController {

  private final PaymentTransactionService paymentTransactionService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> create(
      @Valid @RequestBody PaymentTransactionRequest request) {
    Long response = paymentTransactionService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("PAYMENT_TRANSACTION_CREATED_SUCCESSFULLY", response));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<PaymentTransactionResponse>>> getAllPaged(
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    PageResponse<PaymentTransactionResponse> response =
        paymentTransactionService.getAllTransactionsPaged(startDate, endDate, page, size);
    return ResponseEntity.ok(
        ApiResponse.success("PAYMENT_TRANSACTIONS_RETRIEVED_SUCCESSFULLY", response));
  }

  @GetMapping("/contract/{contractId}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getByContractId(
      @PathVariable Long contractId) {
    List<PaymentTransactionResponse> transactions =
        paymentTransactionService.getTransactionsByContractId(contractId);
    return ResponseEntity.ok(
        ApiResponse.success("PAYMENT_TRANSACTIONS_RETRIEVED_SUCCESSFULLY", transactions));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changeStatus(
      @PathVariable Long id, @RequestParam TransactionStatus newStatus) {
    paymentTransactionService.changeStatus(id, newStatus);
    return ResponseEntity.ok(
        ApiResponse.success("PAYMENT_TRANSACTION_STATUS_UPDATED_SUCCESSFULLY"));
  }

  @GetMapping("/{id}/receipt/pdf")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> generateReceipt(@PathVariable Long id) {
    byte[] pdfBytes = paymentTransactionService.generateReceiptPdf(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "recu_paiement_" + id + ".pdf");
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }
}
