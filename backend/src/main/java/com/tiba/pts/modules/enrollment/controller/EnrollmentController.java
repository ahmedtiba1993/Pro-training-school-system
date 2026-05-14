package com.tiba.pts.modules.enrollment.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentRequest;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentSearchRequest;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentListResponse;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<EnrollmentResponse>> createEnrollment(
      @Valid @RequestBody EnrollmentRequest request) {
    EnrollmentResponse response = enrollmentService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("ENROLLMENT_CREATED_SUCCESSFULLY", response));
  }

  @GetMapping("/{id}")
  public ResponseEntity<EnrollmentResponse> getEnrollmentById(@PathVariable Long id) {
    EnrollmentResponse response = enrollmentService.getById(id);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<PageResponse<EnrollmentListResponse>> getAllEnrollments(
      @ModelAttribute EnrollmentSearchRequest filterRequest) {
    return ResponseEntity.ok(enrollmentService.getAllPaged(filterRequest));
  }

  @PatchMapping("/documents/{submissionId}/toggle-status")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> toggleDocumentStatus(@PathVariable Long submissionId) {

    enrollmentService.toggleDocumentStatus(submissionId);
    return ResponseEntity.ok(ApiResponse.success("DOCUMENT_STATUS_TOGGLED_SUCCESSFULLY", null));
  }

  @PatchMapping("/{id}/status/{status}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> updateStatus(
      @PathVariable Long id, @PathVariable EnrollmentStatus status) {

    enrollmentService.updateStatus(id, status);

    return ResponseEntity.ok(ApiResponse.success("ENROLLMENT_STATUS_UPDATED_SUCCESSFULLY", null));
  }

  @GetMapping("/{id}/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
    byte[] pdfBytes = enrollmentService.exportEnrollmentToPdf(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "fiche_inscription_" + id + ".pdf");
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  @GetMapping("/check-existence")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<Boolean>> checkStudentEnrollmentExistence(
      @RequestParam Long studentId, @RequestParam Long promotionId) {
    boolean isEnrolled = enrollmentService.checkStudentEnrollmentExistence(studentId, promotionId);
    return ResponseEntity.ok(ApiResponse.success("EXISTENCE_CHECK_COMPLETED", isEnrolled));
  }
}
