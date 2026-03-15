package com.tiba.pts.modules.trainingSession.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.trainingSession.dto.TrainingSessionRequest;
import com.tiba.pts.modules.trainingSession.dto.TrainingSessionResponse;
import com.tiba.pts.modules.trainingSession.service.TrainingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training-sessions")
@RequiredArgsConstructor
@Validated
public class TrainingSessionController {

  private final TrainingSessionService trainingSessionService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTrainingSession(
      @Valid @RequestBody TrainingSessionRequest request) {
    Long createdId = trainingSessionService.addTrainingSession(request);
    ApiResponse<Long> response =
        ApiResponse.success("TRAINING_SESSION_CREATED_SUCCESSFULLY", createdId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<TrainingSessionResponse>>>
      getPaginatedTrainingSessions(
          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    PageResponse<TrainingSessionResponse> data = trainingSessionService.getPaginated(page, size);
    ApiResponse<PageResponse<TrainingSessionResponse>> response =
        ApiResponse.success("TRAINING_SESSION_LIST_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTrainingSession(
      @PathVariable Long id, @Valid @RequestBody TrainingSessionRequest request) {
    Long updatedId = trainingSessionService.updateTrainingSession(id, request);
    ApiResponse<Long> response =
        ApiResponse.success("TRAINING_SESSION_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }
}
