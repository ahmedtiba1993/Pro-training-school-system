package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.dto.request.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.response.SpecialtyResponse;
import com.tiba.pts.modules.specialty.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<Long>> updateSpecialty(
      @PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
    Long updatedId = specialtyService.updateSpecialty(id, request);
    ApiResponse<Long> response = ApiResponse.success("UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/all")
  public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> getAllSpecialties() {
    List<SpecialtyResponse> data = specialtyService.getAll();
    ApiResponse<List<SpecialtyResponse>> response =
        ApiResponse.success("ALL_SPECIALTIES_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }
}
