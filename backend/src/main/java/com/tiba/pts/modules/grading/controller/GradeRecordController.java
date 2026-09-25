package com.tiba.pts.modules.grading.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.grading.dto.request.GradeRecordRequest;
import com.tiba.pts.modules.grading.dto.response.GradeRecordResponse;
import com.tiba.pts.modules.grading.service.GradeRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Validated
public class GradeRecordController {

  private final GradeRecordService gradeRecordService;

  @PostMapping("/assessment/{assessmentId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<Void>> addGradeRecords(
      @PathVariable Long assessmentId,
      @RequestBody @NotEmpty(message = "GRADES_LIST_REQUIRED") List<@Valid GradeRecordRequest> requests) {
    gradeRecordService.addGradeRecordsForAssessment(assessmentId, requests);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("GRADES_SAVED_SUCCESSFULLY"));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<Long>> addGradeRecord(
      @Valid @RequestBody GradeRecordRequest request) {
    Long id = gradeRecordService.addGradeRecord(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("GRADE_RECORD_SAVED_SUCCESSFULLY", id));
  }

  @GetMapping("/assessment/{assessmentId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<ApiResponse<List<GradeRecordResponse>>> getAllGradesByAssessment(
      @PathVariable Long assessmentId) {
    List<GradeRecordResponse> response = gradeRecordService.getAllGradesByAssessment(assessmentId);
    return ResponseEntity.ok(ApiResponse.success("GRADES_RETRIEVED_SUCCESSFULLY", response));
  }
}
