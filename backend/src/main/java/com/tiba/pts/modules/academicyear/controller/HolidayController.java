package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.academicyear.dto.request.HolidayRequest;
import com.tiba.pts.modules.academicyear.dto.response.HolidayResponse;
import com.tiba.pts.modules.academicyear.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

  private final HolidayService holidayService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createHoliday(
      @Valid @RequestBody HolidayRequest request) {

    Long holidayId = holidayService.createHoliday(request);
    ApiResponse<Long> response = ApiResponse.success("HOLIDAY_CREATED_SUCCESSFULLY", holidayId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<HolidayResponse>>> getAllHolidaysUnpaged() {

    List<HolidayResponse> data = holidayService.getAllHolidays();
    ApiResponse<List<HolidayResponse>> response =
        ApiResponse.success("HOLIDAY_LIST_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  // Endpoint : GET /api/v1/holidays/academic-year/{academicYearId}
  @GetMapping("/academic-year/{academicYearId}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<HolidayResponse>>> getHolidaysByAcademicYearId(
      @PathVariable Long academicYearId) {

    List<HolidayResponse> data = holidayService.getHolidaysByAcademicYear(academicYearId);
    ApiResponse<List<HolidayResponse>> response =
        ApiResponse.success("HOLIDAYS_FOR_ACADEMIC_YEAR_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<HolidayResponse>> updateHoliday(
      @PathVariable Long id, @Valid @RequestBody HolidayRequest request) {

    HolidayResponse updatedHoliday = holidayService.updateHoliday(id, request);
    ApiResponse<HolidayResponse> response =
        ApiResponse.success("HOLIDAY_UPDATED_SUCCESSFULLY", updatedHoliday);
    return ResponseEntity.ok(response);
  }
}
