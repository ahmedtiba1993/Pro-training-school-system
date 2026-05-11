package com.tiba.pts.modules.trainingsession.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.AcceleratedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AcceleratedPromotionResponse;
import com.tiba.pts.modules.trainingsession.service.AcceleratedPromotionService;
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
@RequestMapping("/api/v1/promotions/accelerated")
@RequiredArgsConstructor
public class AcceleratedPromotionController {

  private final AcceleratedPromotionService service;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createAcceleratedPromotion(
      @Valid @RequestBody AcceleratedPromotionRequest request) {
    Long response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("ACCELERATED_PROMOTION_CREATED_SUCCESSFULLY", response));
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<AcceleratedPromotionResponse>>>
      getAllAcceleratedPromotions(
          @RequestParam(defaultValue = "0") @Min(0) int page,
          @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageResponse<AcceleratedPromotionResponse> paginatedData = service.getAllPaged(page, size);
    ApiResponse<PageResponse<AcceleratedPromotionResponse>> response =
        ApiResponse.success("ACCELERATED_PROMOTION_LIST_RETRIEVED", paginatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AcceleratedPromotionResponse>> getAcceleratedPromotionById(
      @PathVariable Long id) {
    ApiResponse<AcceleratedPromotionResponse> response =
        ApiResponse.success("ACCELERATED_PROMOTION_RETRIEVED_SUCCESSFULLY", service.getById(id));
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateAcceleratedPromotion(
      @PathVariable Long id, @Valid @RequestBody AcceleratedPromotionRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "ACCELERATED_PROMOTION_UPDATED_SUCCESSFULLY", service.update(id, request));
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changeAcceleratedStatus(
      @PathVariable Long id,
      @RequestParam @NotNull(message = "STATUS_REQUIRED") PromotionStatus status) {

    // Appel de la méthode existante dans le service
    service.changeStatus(id, status);

    // Le service renvoie void, donc on passe null à ApiResponse.success
    ApiResponse<Void> response = ApiResponse.success("PROMOTION_STATUS_UPDATED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/status/{status}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<AcceleratedPromotionResponse>>> getPromotionsByStatus(
      @PathVariable PromotionStatus status, @RequestParam(required = false) @Min(1) Integer limit) {
    List<AcceleratedPromotionResponse> data = service.getPromotionsByStatus(status, limit);
    ApiResponse<List<AcceleratedPromotionResponse>> response =
        ApiResponse.success("ACCELERATED_PROMOTIONS_RETRIEVED_SUCCESSFULLY", data);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/status/counts")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Map<PromotionStatus, Long>>> getStatusCounts() {
    Map<PromotionStatus, Long> counts = service.getSpecificStatusCounts();
    ApiResponse<Map<PromotionStatus, Long>> response =
        ApiResponse.success("PROMOTION_STATUS_COUNTS_RETRIEVED_SUCCESSFULLY", counts);
    return ResponseEntity.ok(response);
  }
}
