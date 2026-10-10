package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.dto.request.LevelRequest;
import com.tiba.pts.modules.specialty.dto.response.LevelResponse;
import com.tiba.pts.modules.specialty.service.LevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
@Validated
public class LevelController {

  private final LevelService levelService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createLevel(@Valid @RequestBody LevelRequest request) {
    Long createdId = levelService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("SPECIALTY_LEVEL_CREATED", createdId));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<LevelResponse>>> getAllLevels() {
    List<LevelResponse> data = levelService.getAll();
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LEVEL_LIST_RETRIEVED", data));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<LevelResponse>> getLevelById(@PathVariable Long id) {
    LevelResponse data = levelService.getById(id);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LEVEL_RETRIEVED", data));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateLevel(
      @PathVariable Long id, @Valid @RequestBody LevelRequest request) {
    Long updatedId = levelService.update(id, request);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LEVEL_UPDATED", updatedId));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateLevelStatus(
      @PathVariable Long id, @RequestParam boolean isActive) {
    Long updatedId = levelService.updateStatus(id, isActive);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LEVEL_STATUS_UPDATED", updatedId));
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<LevelResponse>>> getActiveLevels() {
    List<LevelResponse> data = levelService.getAllActive();
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LEVEL_ACTIVE_LIST_RETRIEVED", data));
  }
}
