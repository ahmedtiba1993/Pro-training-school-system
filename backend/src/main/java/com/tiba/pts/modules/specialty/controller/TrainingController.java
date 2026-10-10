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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTraining(
      @Valid @RequestBody TrainingRequest request) {
    Long trainingId = trainingService.createTraining(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("SPECIALTY_TRAINING_CREATED", trainingId));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<PageResponse<TrainingResponse>>> getAllPaged(
      @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC)
          Pageable pageable) {
    PageResponse<TrainingResponse> pageResult = trainingService.getAllPaged(pageable);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_PAGED_RETRIEVED", pageResult));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<TrainingResponse>> getTrainingById(@PathVariable Long id) {
    TrainingResponse data = trainingService.getById(id);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_RETRIEVED", data));
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<TrainingResponse>>> getAllActiveTrainings(
      @RequestParam(required = false) TrainingType type) {
    List<TrainingResponse> data = trainingService.getAllActive(type);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_ACTIVE_RETRIEVED", data));
  }

  @GetMapping("/active/level/{levelId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<TrainingResponse>>> getActiveTrainingsByLevel(
      @PathVariable Long levelId, @RequestParam(required = false) TrainingType type) {
    List<TrainingResponse> data = trainingService.getActiveTrainingsByLevelId(levelId, type);
    return ResponseEntity.ok(
        ApiResponse.success("SPECIALTY_TRAINING_ACTIVE_BY_LEVEL_RETRIEVED", data));
  }

  @GetMapping("/stats/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainingTypeCountResponse>>> getActiveTrainingStats() {
    List<TrainingTypeCountResponse> stats = trainingService.getActiveTrainingStats();
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_STATS_RETRIEVED", stats));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTraining(
      @PathVariable Long id, @Valid @RequestBody TrainingRequest request) {
    Long updatedId = trainingService.updateTraining(id, request);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_UPDATED", updatedId));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTrainingStatus(
      @PathVariable Long id, @RequestParam TrainingStatus status) {
    Long updatedId = trainingService.updateStatus(id, status);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_STATUS_UPDATED", updatedId));
  }

  @GetMapping("/level/{levelId}/ongoing")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<TrainingResponse>>> getOngoingTrainingsByLevel(
      @PathVariable Long levelId) {
    List<TrainingStatus> statusesToFetch =
        List.of(TrainingStatus.DRAFT, TrainingStatus.ACTIVE, TrainingStatus.SUSPENDED);
    List<TrainingResponse> data =
        trainingService.getTrainingsByLevelAndStatuses(levelId, statusesToFetch);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_TRAINING_BY_LEVEL_RETRIEVED", data));
  }
}
