package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.academicyear.dto.request.PeriodRequest;
import com.tiba.pts.modules.academicyear.dto.response.PeriodResponse;
import com.tiba.pts.modules.academicyear.service.PeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/periods")
@RequiredArgsConstructor
public class PeriodController {

  private final PeriodService periodService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createPeriod(@Valid @RequestBody PeriodRequest request) {
    Long periodId = periodService.createPeriod(request);
    ApiResponse<Long> response = ApiResponse.success("PERIOD_CREATED_SUCCESSFULLY", periodId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/academic-year/{academicYearId}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<PeriodResponse>>> getPeriodsByAcademicYearId(
      @PathVariable Long academicYearId) {
    List<PeriodResponse> data = periodService.getPeriodsByAcademicYear(academicYearId);
    ApiResponse<List<PeriodResponse>> response =
        ApiResponse.success("PERIODS_FOR_YEAR_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PeriodResponse>> updatePeriod(
      @PathVariable Long id, @Valid @RequestBody PeriodRequest request) {

    PeriodResponse updatedPeriod = periodService.updatePeriod(id, request);
    ApiResponse<PeriodResponse> response =
        ApiResponse.success("PERIOD_UPDATED_SUCCESSFULLY", updatedPeriod);

    return ResponseEntity.ok(response);
  }
}
