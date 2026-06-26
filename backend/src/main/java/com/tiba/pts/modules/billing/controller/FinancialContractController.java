package com.tiba.pts.modules.billing.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.billing.dto.request.ActivateContractRequest;
import com.tiba.pts.modules.billing.dto.request.FinancialContractSearchRequest;
import com.tiba.pts.modules.billing.dto.response.ActiveRevenueStatResponse;
import com.tiba.pts.modules.billing.dto.response.FinancialContractListResponse;
import com.tiba.pts.modules.billing.service.FinancialContractService;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial-contracts")
@RequiredArgsConstructor
public class FinancialContractController {

  private final FinancialContractService financialContractService;

  @GetMapping("/paged")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<FinancialContractListResponse>>>
      getAllFinancialContractsPaged(@ModelAttribute FinancialContractSearchRequest searchParams) {

    PageResponse<FinancialContractListResponse> response =
        financialContractService.getAllContractsPaged(searchParams);
    return ResponseEntity.ok(
        ApiResponse.success("FINANCIAL_CONTRACTS_RETRIEVED_SUCCESSFULLY", response));
  }

  @GetMapping("/stats/active-revenue")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<ActiveRevenueStatResponse>> getActiveRevenueStat(
      @RequestParam(required = false) TrainingType trainingType) {

    ActiveRevenueStatResponse response =
        financialContractService.getActiveRevenueStat(trainingType);
    return ResponseEntity.ok(
        ApiResponse.success("ACTIVE_REVENUE_STAT_RETRIEVED_SUCCESSFULLY", response));
  }

  @PutMapping("/{id}/activate")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> activateContract(
      @PathVariable Long id, @Valid @RequestBody ActivateContractRequest request) {

    financialContractService.activateContract(
        id,
        request.getDiscountAmount(),
        request.getPaymentMethod(),
        request.getReference(),
        request.getPaymentDate());
    return ResponseEntity.ok(
        ApiResponse.success("FINANCIAL_CONTRACT_ACTIVATED_SUCCESSFULLY"));
  }
}

