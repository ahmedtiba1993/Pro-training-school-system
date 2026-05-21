package com.tiba.pts.modules.trainingsession.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.trainingsession.dto.request.PromotionSubjectRequest;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionStatsResponse;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionSubjectResponse;
import com.tiba.pts.modules.trainingsession.service.PromotionSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotion-subjects")
@RequiredArgsConstructor
public class PromotionSubjectController {

  private final PromotionSubjectService promotionSubjectService;

  /**
   * Endpoint to assign a subject to a promotion with its coefficient. Restricted to
   * Administration and Secretariat .
   */
  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> create(
      @Valid @RequestBody PromotionSubjectRequest request) {

    Long data = promotionSubjectService.create(request);

    ApiResponse<Long> response =
        ApiResponse.success("PROMOTION_SUBJECT_CREATED_SUCCESSFULLY", data);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Endpoint to retrieve the refined list of subjects assigned to a specific promotion.
   * Accessible by all system roles for viewing
   */
  @GetMapping("/promotion/{promotionId}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<PromotionSubjectResponse>>> getSubjectsByPromotionId(
      @PathVariable Long promotionId) {

    List<PromotionSubjectResponse> data =
        promotionSubjectService.getSubjectsByPromotion(promotionId);

    ApiResponse<List<PromotionSubjectResponse>> response =
        ApiResponse.success("PROMOTION_SUBJECTS_RETRIEVED_SUCCESSFULLY", data);

    return ResponseEntity.ok(response);
  }


  /** Endpoint to obtain the global statistical summary of a promotion (all types combined). */
  @GetMapping("/promotion/{promotionId}/stats")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SECRETARIAT', 'ROLE_FORMATEUR', 'ROLE_APPRENANT')")
  public ResponseEntity<ApiResponse<PromotionStatsResponse>> getPromotionStats(
      @PathVariable Long promotionId) {

    PromotionStatsResponse data = promotionSubjectService.getPromotionStats(promotionId);
    ApiResponse<PromotionStatsResponse> response =
        ApiResponse.success("PROMOTION_STATS_RETRIEVED_SUCCESSFULLY", data);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/promotion/{promotionId}/period/{periodId}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<PromotionSubjectResponse>>>
      getSubjectsByPromotionAndPeriod(@PathVariable Long promotionId, @PathVariable Long periodId) {

    List<PromotionSubjectResponse> data =
        promotionSubjectService.getSubjectsByPromotionAndPeriod(promotionId, periodId);

    return ResponseEntity.ok(
        ApiResponse.success("SUBJECTS_FOR_PROMOTION_AND_PERIOD_RETRIEVED", data));
  }

  /**
   * Endpoint to delete the assignment of a subject to a promotion. Secure operation and
   * restricted to administrators and secretariat.
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deletePromotionSubject(@PathVariable Long id) {

    promotionSubjectService.delete(id);

    ApiResponse<Void> response =
        ApiResponse.success("PROMOTION_SUBJECT_DELETED_SUCCESSFULLY", null);

    return ResponseEntity.ok(response);
  }
}
