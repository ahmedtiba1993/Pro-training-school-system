package com.tiba.pts.modules.trainingsession.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionUpdateRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionResponse;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionStatsResponse;
import com.tiba.pts.modules.trainingsession.dto.response.OngoingPromotionResponse;
import com.tiba.pts.modules.trainingsession.service.AccreditedPromotionService;
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
@RequestMapping("/api/v1/promotions/accredited")
@RequiredArgsConstructor
public class AccreditedPromotionController {

  private final AccreditedPromotionService service;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createAccreditedPromotion(
      @Valid @RequestBody AccreditedPromotionRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "ACCREDITED_PROMOTION_CREATED_SUCCESSFULLY",
            service.createAccreditedPromotion(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<AccreditedPromotionResponse>>>
      getAllAccreditedPromotions(
          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    PageResponse<AccreditedPromotionResponse> paginatedData = service.getAllPaged(page, size);
    ApiResponse<PageResponse<AccreditedPromotionResponse>> response =
        ApiResponse.success("ACCREDITED_PROMOTION_LIST_RETRIEVED", paginatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AccreditedPromotionResponse>> getAccreditedPromotionById(
      @PathVariable Long id) {
    ApiResponse<AccreditedPromotionResponse> response =
        ApiResponse.success("ACCREDITED_PROMOTION_RETRIEVED_SUCCESSFULLY", service.getById(id));
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateAccreditedPromotion(
      @PathVariable Long id, @Valid @RequestBody AccreditedPromotionUpdateRequest request) {
    service.updateAccreditedPromotion(id, request);
    ApiResponse<Long> response =
        ApiResponse.success("ACCREDITED_PROMOTION_UPDATED_SUCCESSFULLY", id);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/status/{status}")
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<List<AccreditedPromotionResponse>>> getPromotionsByStatus(
      @PathVariable PromotionStatus status, @RequestParam(required = false) @Min(1) Integer limit) {
    List<AccreditedPromotionResponse> data = service.getPromotionsByStatus(status, limit);
    ApiResponse<List<AccreditedPromotionResponse>> response =
        ApiResponse.success("ACCREDITED_PROMOTIONS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changePromotionStatus(
      @PathVariable Long id,
      @RequestParam @Valid @NotNull(message = "STATUS_IS_REQUIRED") PromotionStatus status) {
    service.changeStatus(id, status);
    ApiResponse<Void> response = ApiResponse.success("PROMOTION_STATUS_UPDATED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/statistics")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<List<AccreditedPromotionStatsResponse>>>
      getAccreditedPromotionStatistics() {
    List<AccreditedPromotionStatsResponse> data = service.getPromotionStatistics();
    ApiResponse<List<AccreditedPromotionStatsResponse>> response =
        ApiResponse.success("PROMOTION_STATISTICS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/ongoing")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<OngoingPromotionResponse>>> getOngoingPromotions(
      @RequestParam(required = false) @Min(value = 1, message = "LIMIT_MUST_BE_POSITIVE")
          Integer limit) {
    List<OngoingPromotionResponse> data = service.getOngoingPromotions(limit);
    ApiResponse<List<OngoingPromotionResponse>> response =
        ApiResponse.success("ONGOING_PROMOTIONS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }
}
