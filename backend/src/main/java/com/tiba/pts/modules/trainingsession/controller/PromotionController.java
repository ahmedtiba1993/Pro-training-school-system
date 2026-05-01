package com.tiba.pts.modules.trainingsession.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionLookupResponse;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionStatisticsResponse;
import com.tiba.pts.modules.trainingsession.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

  private final PromotionService promotionService;

  @GetMapping("/statistics")
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<PromotionStatisticsResponse>> getPromotionStatistics(
      @RequestParam(required = false) TrainingType trainingType) {

    PromotionStatisticsResponse stats = promotionService.getStatistics(trainingType);

    ApiResponse<PromotionStatisticsResponse> response =
        ApiResponse.success("PROMOTION_STATISTICS_RETRIEVED_SUCCESSFULLY", stats);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/training/{trainingId}/open-enrollment/lookup")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<PromotionLookupResponse>>> getOpenPromotionsLookup(
      @PathVariable Long trainingId) {

    List<PromotionLookupResponse> lookupData =
        promotionService.getOpenPromotionsLookupByTrainingId(trainingId);

    return ResponseEntity.ok(ApiResponse.success("PROMOTION_LOOKUP_RETRIEVED", lookupData));
  }
}
