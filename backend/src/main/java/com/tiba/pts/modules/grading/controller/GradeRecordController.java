package com.tiba.pts.modules.grading.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.grading.dto.request.AssessmentGradesRequest;
import com.tiba.pts.modules.grading.service.GradeRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Validated
public class GradeRecordController {

  private final GradeRecordService gradeRecordService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> saveGrades(
      @Valid @RequestBody AssessmentGradesRequest request) {
    gradeRecordService.saveGrades(request);
    return ResponseEntity.ok(ApiResponse.success("GRADES_SAVED_SUCCESSFULLY"));
  }
}
