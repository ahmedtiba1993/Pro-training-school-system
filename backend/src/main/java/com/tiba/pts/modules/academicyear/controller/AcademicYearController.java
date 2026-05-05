package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.dto.request.AcademicYearRequest;
import com.tiba.pts.modules.academicyear.dto.response.AcademicYearResponse;
import com.tiba.pts.modules.academicyear.dto.response.ActiveAcademicYearResponse;
import com.tiba.pts.modules.academicyear.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@RequiredArgsConstructor
@Validated
public class AcademicYearController {

  private final AcademicYearService academicYearService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createAcademicYear(
      @Valid @RequestBody AcademicYearRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "ACADEMIC_YEAR_CREATED_SUCCESSFULLY", academicYearService.createAcademicYear(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<AcademicYearResponse>>> getAllAcademicYears(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    PageResponse<AcademicYearResponse> paginatedData = academicYearService.getAllPaged(page, size);
    ApiResponse<PageResponse<AcademicYearResponse>> response =
        ApiResponse.success("ACADEMIC_YEAR_LIST_RETRIEVED", paginatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AcademicYearResponse>> getAcademicYearById(
      @PathVariable Long id) {
    AcademicYearResponse data = academicYearService.getById(id);
    ApiResponse<AcademicYearResponse> response = ApiResponse.success("ACADEMIC_YEAR_FOUND", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateAcademicYear(
      @PathVariable Long id, @Valid @RequestBody AcademicYearRequest request) {
    Long updatedId = academicYearService.updateAcademicYear(id, request);
    ApiResponse<Long> response =
        ApiResponse.success("ACADEMIC_YEAR_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/set-default")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> setDefaultAcademicYear(@PathVariable Long id) {
    academicYearService.setDefaultAcademicYear(id);
    return ResponseEntity.ok(ApiResponse.success("ACADEMIC_YEAR_SET_AS_DEFAULT"));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changeAcademicYearStatus(
      @PathVariable Long id, @RequestParam YearStatus newStatus) {
    academicYearService.changeStatus(id, newStatus);
    return ResponseEntity.ok(ApiResponse.success("ACADEMIC_YEAR_STATUS_CHANGED"));
  }

  @PatchMapping("/{id}/toggle-lock")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> toggleLock(@PathVariable Long id) {
    academicYearService.toggleLock(id);
    return ResponseEntity.ok(ApiResponse.success("ACADEMIC_YEAR_LOCK_STATUS_TOGGLED"));
  }

  @GetMapping("/open")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> getOpenAcademicYears() {
    List<AcademicYearResponse> openYears = academicYearService.getOpenAcademicYears();
    ApiResponse<List<AcademicYearResponse>> response =
        ApiResponse.success("OPEN_ACADEMIC_YEARS_RETRIEVED", openYears);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/current")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<ActiveAcademicYearResponse>> getCurrentAcademicYear() {
    ActiveAcademicYearResponse currentYear = academicYearService.getCurrentAcademicYear();
    ApiResponse<ActiveAcademicYearResponse> response =
        ApiResponse.success("CURRENT_ACADEMIC_YEAR_RETRIEVED", currentYear);
    return ResponseEntity.ok(response);
  }
}
