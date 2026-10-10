package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.dto.request.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.response.SpecialtyResponse;
import com.tiba.pts.modules.specialty.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

  private final SpecialtyService specialtyService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createSpecialty(
      @Valid @RequestBody SpecialtyRequest request) {
    Long id = specialtyService.createSpecialty(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("SPECIALTY_CREATED", id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateSpecialty(
      @PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
    Long updatedId = specialtyService.updateSpecialty(id, request);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_UPDATED", updatedId));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getAllSpecialties() {
    List<SpecialtyResponse> data = specialtyService.getAll();
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LIST_RETRIEVED", data));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<SpecialtyResponse>> getSpecialtyById(@PathVariable Long id) {
    SpecialtyResponse data = specialtyService.getById(id);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_RETRIEVED", data));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateSpecialtyStatus(
      @PathVariable Long id, @RequestParam boolean isActive) {
    Long updatedId = specialtyService.updateStatus(id, isActive);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_STATUS_UPDATED", updatedId));
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getActiveSpecialties() {
    List<SpecialtyResponse> data = specialtyService.getAllActive();
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_ACTIVE_LIST_RETRIEVED", data));
  }
}
