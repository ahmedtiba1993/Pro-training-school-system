package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.specialty.dto.request.TrainingRequest;
import com.tiba.pts.modules.specialty.dto.response.TrainingResponse;
import com.tiba.pts.modules.specialty.dto.response.TrainingTypeCountResponse;
import com.tiba.pts.modules.specialty.service.TrainingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTraining(
      @Valid @RequestBody TrainingRequest request) {
    Long trainingId = trainingService.createTraining(request);
    ApiResponse<Long> response = ApiResponse.success("TRAINING_CREATED_SUCCESSFULLY", trainingId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<TrainingResponse>>> getAllPaged(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageResponse<TrainingResponse> paginatedData = trainingService.getAllPaged(page, size);
    ApiResponse<PageResponse<TrainingResponse>> response =
        ApiResponse.success("TRAINING_LIST_RETRIEVED", paginatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<TrainingResponse>>> getAllActiveTrainings() {
    List<TrainingResponse> activeTrainings = trainingService.getAllActive();
    ApiResponse<List<TrainingResponse>> response =
        ApiResponse.success("ACTIVE_TRAINING_LIST_RETRIEVED", activeTrainings);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/change-activation")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Boolean>> changeTrainingActivation(@PathVariable Long id) {
    boolean newStatus = trainingService.changeActivation(id);
    String message =
        newStatus ? "TRAINING_ACTIVATED_SUCCESSFULLY" : "TRAINING_DEACTIVATED_SUCCESSFULLY";
    ApiResponse<Boolean> response = ApiResponse.success(message);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTraining(
      @PathVariable Long id, @Valid @RequestBody TrainingRequest request) {
    Long updatedId = trainingService.updateTraining(id, request);
    ApiResponse<Long> response = ApiResponse.success("TRAINING_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/active-by-type")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<TrainingTypeCountResponse>>> getActiveTrainingStats() {

    List<TrainingTypeCountResponse> stats = trainingService.getActiveTrainingStats();

    ApiResponse<List<TrainingTypeCountResponse>> response =
        ApiResponse.success("TRAINING_STATS_RETRIEVED", stats);

    return ResponseEntity.ok(response);
  }
}
