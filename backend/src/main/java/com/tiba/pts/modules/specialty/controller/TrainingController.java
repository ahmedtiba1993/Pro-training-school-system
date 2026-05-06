package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.dto.request.TrainingRequest;
import com.tiba.pts.modules.specialty.dto.response.TrainingResponse;
import com.tiba.pts.modules.specialty.dto.response.TrainingTypeCountResponse;
import com.tiba.pts.modules.specialty.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trainings")
@RequiredArgsConstructor
@Validated
public class TrainingController {

  private final TrainingService trainingService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTraining(
      @Valid @RequestBody TrainingRequest request) {
    Long trainingId = trainingService.createTraining(request);
    ApiResponse<Long> response = ApiResponse.success("TRAINING_CREATED_SUCCESSFULLY", trainingId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<TrainingResponse>>> getAllPaged(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    PageResponse<TrainingResponse> pageResult = trainingService.getAllPaged(page, size);
    ApiResponse<PageResponse<TrainingResponse>> response =
        ApiResponse.success("TRAININGS_PAGED_RETRIEVED", pageResult);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainingResponse>>> getAllActiveTrainings(
      @RequestParam(required = false) TrainingType type) {
    List<TrainingResponse> data = trainingService.getAllActive(type);
    ApiResponse<List<TrainingResponse>> response =
        ApiResponse.success("ACTIVE_TRAININGS_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active/level/{levelId}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainingResponse>>> getActiveTrainingsByLevel(
      @PathVariable Long levelId) {
    List<TrainingResponse> data = trainingService.getActiveTrainingsByLevelId(levelId);
    ApiResponse<List<TrainingResponse>> response =
        ApiResponse.success("ACTIVE_TRAININGS_BY_LEVEL_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/active")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainingTypeCountResponse>>> getActiveTrainingStats() {
    List<TrainingTypeCountResponse> stats = trainingService.getActiveTrainingStats();
    ApiResponse<List<TrainingTypeCountResponse>> response =
        ApiResponse.success("ACTIVE_TRAINING_STATS_RETRIEVED", stats);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTraining(
      @PathVariable Long id, @Valid @RequestBody TrainingRequest request) {
    Long updatedId = trainingService.updateTraining(id, request);
    ApiResponse<Long> response = ApiResponse.success("TRAINING_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<Long>> updateTrainingStatus(
      @PathVariable Long id, @RequestParam TrainingStatus status) {
    Long updatedId = trainingService.updateStatus(id, status);
    ApiResponse<Long> response =
        ApiResponse.success("TRAINING_STATUS_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }
}
