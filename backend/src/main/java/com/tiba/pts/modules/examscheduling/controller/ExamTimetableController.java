package com.tiba.pts.modules.examscheduling.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.examscheduling.dto.request.ExamTimetableRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamTimetableResponse;
import com.tiba.pts.modules.examscheduling.service.ExamTimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam-timetables")
@RequiredArgsConstructor
@Validated
public class ExamTimetableController {

  private final ExamTimetableService examTimetableService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTimetable(
      @Valid @RequestBody ExamTimetableRequest request) {
    Long id = examTimetableService.createTimetable(request);
    ApiResponse<Long> response = ApiResponse.success("EXAM_TIMETABLE_CREATED_SUCCESSFULLY", id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{id}/publish")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> publishTimetable(@PathVariable Long id) {
    examTimetableService.publishTimetable(id);
    ApiResponse<Void> response = ApiResponse.success("EXAM_TIMETABLE_PUBLISHED_SUCCESSFULLY");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ExamTimetableResponse>> getTimetableById(@PathVariable Long id) {
    ExamTimetableResponse timetable = examTimetableService.getTimetableById(id);
    ApiResponse<ExamTimetableResponse> response =
        ApiResponse.success("EXAM_TIMETABLE_RETRIEVED", timetable);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<java.util.List<ExamTimetableResponse>>> getAllTimetables() {
    java.util.List<ExamTimetableResponse> list = examTimetableService.getAllTimetables();
    ApiResponse<java.util.List<ExamTimetableResponse>> response =
        ApiResponse.success("EXAM_TIMETABLES_RETRIEVED", list);
    return ResponseEntity.ok(response);
  }
}
