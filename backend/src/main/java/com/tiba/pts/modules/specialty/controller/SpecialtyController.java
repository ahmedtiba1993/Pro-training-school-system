package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.dto.SpecialtyRequest;
import com.tiba.pts.modules.specialty.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
