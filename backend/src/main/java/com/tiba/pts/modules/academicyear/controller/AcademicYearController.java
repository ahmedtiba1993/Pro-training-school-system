package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.academicyear.dto.AcademicYearDto;
import com.tiba.pts.modules.academicyear.dto.ActiveAcademicYearDTO;
import com.tiba.pts.modules.academicyear.service.AcademicYearService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
@Validated
public class AcademicYearController {

  private final AcademicYearService academicYearService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createAcademicYear(
      @Valid @RequestBody AcademicYearDto request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "ACADEMIC_YEAR_CREATED_SUCCESSFULLY", academicYearService.create(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<AcademicYearDto>>> getAllAcademicYears(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    PageResponse<AcademicYearDto> paginatedData = academicYearService.getAll(page, size);
    ApiResponse<PageResponse<AcademicYearDto>> response =
        ApiResponse.success("ACADEMIC_YEAR_LIST_RETRIEVED", paginatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AcademicYearDto>> getAcademicYearById(@PathVariable Long id) {
    AcademicYearDto data = academicYearService.getById(id);
    ApiResponse<AcademicYearDto> response = ApiResponse.success("ACADEMIC_YEAR_FOUND", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AcademicYearDto>> updateAcademicYear(
      @PathVariable Long id, @Valid @RequestBody AcademicYearDto request) {
    AcademicYearDto data = academicYearService.update(id, request);
    ApiResponse<AcademicYearDto> response = ApiResponse.success("ACADEMIC_YEAR_UPDATED", data);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/activate")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> activateAcademicYear(@PathVariable Long id) {
    academicYearService.activateYear(id);
    ApiResponse<Void> response = ApiResponse.success("ACADEMIC_YEAR_ACTIVATED");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AcademicYearDto>> getActiveAcademicYear() {
    AcademicYearDto data = academicYearService.getActiveYear();
    ApiResponse<AcademicYearDto> response =
        ApiResponse.success("ACTIVE_ACADEMIC_YEAR_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/current-session")
  public ResponseEntity<ApiResponse<ActiveAcademicYearDTO>> getCurrentSession() {
    ActiveAcademicYearDTO activeAcademicYearDTO = academicYearService.getCurrentSession();
    ApiResponse<ActiveAcademicYearDTO> response =
        ApiResponse.success("CURRENT_SESSION_RETRIEVED", activeAcademicYearDTO);
    return ResponseEntity.ok(response);
  }
}
