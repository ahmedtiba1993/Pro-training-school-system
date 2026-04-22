package com.tiba.pts.modules.trainingsession.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.ContinuousPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.ContinuousPromotionResponse;
import com.tiba.pts.modules.trainingsession.service.ContinuousPromotionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/continuous")
@RequiredArgsConstructor
public class ContinuousPromotionController {

  private final ContinuousPromotionService service;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createContinuousPromotion(
      @Valid @RequestBody ContinuousPromotionRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "CONTINUOUS_PROMOTION_CREATED_SUCCESSFULLY",
            service.createContinuousPromotion(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<ContinuousPromotionResponse>>>
      getAllContinuousPromotions(
          @RequestParam(defaultValue = "0") @Min(0) int page,
          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageResponse<ContinuousPromotionResponse> paginatedData = service.getAllPaged(page, size);
    ApiResponse<PageResponse<ContinuousPromotionResponse>> response =
        ApiResponse.success("CONTINUOUS_PROMOTION_LIST_RETRIEVED", paginatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ContinuousPromotionResponse>> getById(@PathVariable Long id) {
    ApiResponse<ContinuousPromotionResponse> response =
        ApiResponse.success("CONTINUOUS_PROMOTION_RETRIEVED_SUCCESSFULLY", service.getById(id));
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateContinuousPromotion(
      @PathVariable Long id, @Valid @RequestBody ContinuousPromotionRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "CONTINUOUS_PROMOTION_UPDATED_SUCCESSFULLY", service.update(id, request));
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> updatePromotionStatus(
      @PathVariable Long id,
      @RequestParam @Valid @NotNull(message = "STATUS_IS_REQUIRED") PromotionStatus status) {
    service.updateStatus(id, status);
    ApiResponse<Void> response = ApiResponse.success("PROMOTION_STATUS_UPDATED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/status/{status}")
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<List<ContinuousPromotionResponse>>> getPromotionsByStatus(
      @PathVariable PromotionStatus status, @RequestParam(required = false) @Min(1) Integer limit) {
    List<ContinuousPromotionResponse> data = service.getPromotionsByStatus(status, limit);
    ApiResponse<List<ContinuousPromotionResponse>> response =
        ApiResponse.success("CONTINUOUS_PROMOTIONS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }
}
