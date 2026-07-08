package com.tiba.pts.modules.grading.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.grading.dto.request.AssessmentRequest;
import com.tiba.pts.modules.grading.dto.response.AssessmentLookupResponse;
import com.tiba.pts.modules.grading.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@Validated
public class AssessmentController {

  private final AssessmentService assessmentService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createAssessment(
      @Valid @RequestBody AssessmentRequest request) {
    Long id = assessmentService.createAssessment(request);
    ApiResponse<Long> response = ApiResponse.success("ASSESSMENT_CREATED_SUCCESSFULLY", id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/timetable/{timetableId}/unscheduled")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<AssessmentLookupResponse>>> getUnscheduledAssessmentsForTimetable(
      @PathVariable Long timetableId) {
    List<AssessmentLookupResponse> list = assessmentService.getUnscheduledAssessmentsForTimetable(timetableId);
    ApiResponse<List<AssessmentLookupResponse>> response =
        ApiResponse.success("UNSCHEDULED_ASSESSMENTS_RETRIEVED", list);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/start-grading")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> startGrading(@PathVariable Long id) {
    assessmentService.startGrading(id);
    return ResponseEntity.ok(ApiResponse.success("ASSESSMENT_GRADING_STARTED_SUCCESSFULLY"));
  }

  @PatchMapping("/{id}/submit")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> submit(@PathVariable Long id) {
    assessmentService.submit(id);
    return ResponseEntity.ok(ApiResponse.success("ASSESSMENT_SUBMITTED_SUCCESSFULLY"));
  }

  @PatchMapping("/{id}/lock")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> lock(@PathVariable Long id) {
    assessmentService.lock(id);
    return ResponseEntity.ok(ApiResponse.success("ASSESSMENT_LOCKED_SUCCESSFULLY"));
  }
}
