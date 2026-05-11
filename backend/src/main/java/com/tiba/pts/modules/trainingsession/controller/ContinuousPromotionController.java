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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/promotions/continuous")
@RequiredArgsConstructor
public class ContinuousPromotionController {

  private final ContinuousPromotionService service;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createContinuousPromotion(
      @Valid @RequestBody ContinuousPromotionRequest request) {
    Long response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("CONTINUOUS_PROMOTION_CREATED_SUCCESSFULLY", response));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<ContinuousPromotionResponse>>>
      getAllContinuousPromotions(
          @RequestParam(defaultValue = "0") @Min(0) int page,
          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageResponse<ContinuousPromotionResponse> paginatedData = service.getAllPaged(page, size);
    return ResponseEntity.ok(
        ApiResponse.success("CONTINUOUS_PROMOTION_LIST_RETRIEVED", paginatedData));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<ContinuousPromotionResponse>> getContinuousPromotionById(
      @PathVariable Long id) {
    ContinuousPromotionResponse data = service.getById(id);
    return ResponseEntity.ok(
        ApiResponse.success("CONTINUOUS_PROMOTION_RETRIEVED_SUCCESSFULLY", data));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateContinuousPromotion(
      @PathVariable Long id, @Valid @RequestBody ContinuousPromotionRequest request) {
    Long response = service.update(id, request);
    return ResponseEntity.ok(
        ApiResponse.success("CONTINUOUS_PROMOTION_UPDATED_SUCCESSFULLY", response));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changeContinuousStatus(
      @PathVariable Long id,
      @RequestParam @NotNull(message = "STATUS_REQUIRED") PromotionStatus status) {
    service.changeStatus(id, status);
    return ResponseEntity.ok(ApiResponse.success("PROMOTION_STATUS_UPDATED_SUCCESSFULLY", null));
  }

  @GetMapping("/status/{status}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<ContinuousPromotionResponse>>>
      getContinuousPromotionByStatus(
          @PathVariable PromotionStatus status,
          @RequestParam(required = false) @Min(1) Integer limit) {
    List<ContinuousPromotionResponse> data = service.getPromotionsByStatus(status, limit);
    return ResponseEntity.ok(
        ApiResponse.success("CONTINUOUS_PROMOTIONS_RETRIEVED_SUCCESSFULLY", data));
  }

  @GetMapping("/status/counts")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Map<PromotionStatus, Long>>>
      getContinuousPromotionStatusCounts() {
    Map<PromotionStatus, Long> counts = service.getSpecificStatusCounts();
    return ResponseEntity.ok(
        ApiResponse.success("PROMOTION_STATUS_COUNTS_RETRIEVED_SUCCESSFULLY", counts));
  }
}
