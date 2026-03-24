package com.tiba.pts.modules.enrollment.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentRequest;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.service.EnrollmentService;
// Importez ici votre classe ApiResponse (ex: com.tiba.pts.common.dto.ApiResponse)
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createEnrollment(
      @Valid @RequestBody EnrollmentRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "ENROLLMENT_CREATED_SUCCESSFULLY", enrollmentService.createEnrollment(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getAllEnrollments(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    PageResponse<EnrollmentResponse> pageResponse = enrollmentService.getAll(page, size);
    ApiResponse<PageResponse<EnrollmentResponse>> response =
        ApiResponse.success("ENROLLMENTS_FETCHED_SUCCESSFULLY", pageResponse);
    return ResponseEntity.ok(response);
  }
}
