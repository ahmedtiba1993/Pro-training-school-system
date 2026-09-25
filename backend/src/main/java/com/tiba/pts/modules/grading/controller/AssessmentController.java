package com.tiba.pts.modules.grading.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.grading.dto.request.AssessmentRequest;
import com.tiba.pts.modules.grading.dto.response.AssessmentLookupResponse;
import com.tiba.pts.modules.grading.dto.response.AssessmentResponse;
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

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateAssessment(
      @PathVariable Long id, @Valid @RequestBody AssessmentRequest request) {
    Long updatedId = assessmentService.updateAssessment(id, request);
    ApiResponse<Long> response = ApiResponse.success("ASSESSMENT_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/promotion-subject/{promotionSubjectId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getAssessmentsByPromotionSubject(
      @PathVariable Long promotionSubjectId) {
    List<AssessmentResponse> list =
        assessmentService.getAssessmentsByPromotionSubject(promotionSubjectId);
    ApiResponse<List<AssessmentResponse>> response =
        ApiResponse.success("ASSESSMENTS_RETRIEVED_SUCCESSFULLY", list);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/promotion-subject/{promotionSubjectId}/grading")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getGradingAssessmentsByPromotionSubject(
      @PathVariable Long promotionSubjectId) {
    List<AssessmentResponse> list =
        assessmentService.getGradingAssessmentsByPromotionSubject(promotionSubjectId);
    ApiResponse<List<AssessmentResponse>> response =
        ApiResponse.success("ASSESSMENTS_RETRIEVED_SUCCESSFULLY", list);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/timetable/{timetableId}/unscheduled")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<AssessmentLookupResponse>>>
      getUnscheduledAssessmentsForTimetable(@PathVariable Long timetableId) {
    List<AssessmentLookupResponse> list =
        assessmentService.getUnscheduledAssessmentsForTimetable(timetableId);
    ApiResponse<List<AssessmentLookupResponse>> response =
        ApiResponse.success("UNSCHEDULED_ASSESSMENTS_RETRIEVED", list);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteAssessment(@PathVariable Long id) {
    assessmentService.deleteAssessment(id);
    ApiResponse<Void> response = ApiResponse.success("ASSESSMENT_DELETED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/mark-planned")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> markAsPlanned(@PathVariable Long id) {
    assessmentService.markAsPlanned(id);
    ApiResponse<Void> response =
        ApiResponse.success("ASSESSMENT_MARKED_AS_PLANNED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/open-grading")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> openForGrading(@PathVariable Long id) {
    assessmentService.openForGrading(id);
    ApiResponse<Void> response =
        ApiResponse.success("ASSESSMENT_OPENED_FOR_GRADING_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/submit-to-admin")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<Void>> submitToAdmin(@PathVariable Long id) {
    assessmentService.submitToAdmin(id);
    ApiResponse<Void> response =
        ApiResponse.success("ASSESSMENT_SUBMITTED_TO_ADMIN_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/reject-return-to-teacher")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> rejectAndReturnToTeacher(@PathVariable Long id) {
    assessmentService.rejectAndReturnToTeacher(id);
    ApiResponse<Void> response =
        ApiResponse.success("ASSESSMENT_REJECTED_AND_RETURNED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/publish")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> publishAssessment(@PathVariable Long id) {
    assessmentService.publishAssessment(id);
    ApiResponse<Void> response = ApiResponse.success("ASSESSMENT_PUBLISHED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> cancelAssessment(@PathVariable Long id) {
    assessmentService.cancelAssessment(id);
    ApiResponse<Void> response = ApiResponse.success("ASSESSMENT_CANCELLED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/lock")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> lockAssessment(@PathVariable Long id) {
    assessmentService.lockAssessment(id);
    ApiResponse<Void> response = ApiResponse.success("ASSESSMENT_LOCKED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }
}
