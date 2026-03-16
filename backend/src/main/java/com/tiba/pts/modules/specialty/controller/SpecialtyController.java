package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.dto.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.SpecialtyResponse;
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
  public ResponseEntity<ApiResponse<Long>> createSpecialty(
      @Valid @RequestBody SpecialtyRequest request) {
    Long id = specialtyService.createSpecialty(request);
    ApiResponse<Long> response = ApiResponse.success("CREATED_SUCCESSFULLY", id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getAllSpecialties() {
    List<SpecialtyResponse> specialties = specialtyService.getAllSpecialties();
    ApiResponse<List<SpecialtyResponse>> response =
        ApiResponse.success("CREATED_SUCCESSFULLY", specialties);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Long>> updateSpecialty(
      @PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
    Long updatedId = specialtyService.updateSpecialty(id, request);
    ApiResponse<Long> response = ApiResponse.success("UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/by-level/{levelId}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getSpecialtiesByLevel(
      @PathVariable Long levelId) {
    List<SpecialtyResponse> data = specialtyService.getSpecialtiesByLevelId(levelId);
    ApiResponse<List<SpecialtyResponse>> response =
        ApiResponse.success("SPECIALTIES_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }
}
