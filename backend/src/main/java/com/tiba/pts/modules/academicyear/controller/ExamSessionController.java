package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.academicyear.dto.request.ExamSessionRequest;
import com.tiba.pts.modules.academicyear.dto.response.ExamSessionResponse;
import com.tiba.pts.modules.academicyear.service.ExamSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-sessions")
@RequiredArgsConstructor
public class ExamSessionController {

  private final ExamSessionService examSessionService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createExamSession(
      @Valid @RequestBody ExamSessionRequest request) {

    Long sessionId = examSessionService.createExamSession(request);
    ApiResponse<Long> response =
        ApiResponse.success("EXAM_SESSION_CREATED_SUCCESSFULLY", sessionId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/period/{periodId}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ExamSessionResponse>>> getExamSessionsByPeriodId(
      @PathVariable Long periodId) {

    List<ExamSessionResponse> data = examSessionService.getExamSessionsByPeriod(periodId);
    ApiResponse<List<ExamSessionResponse>> response =
        ApiResponse.success("EXAM_SESSIONS_FOR_PERIOD_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ExamSessionResponse>> updateExamSession(
      @PathVariable Long id, @Valid @RequestBody ExamSessionRequest request) {

    ExamSessionResponse updatedSession = examSessionService.updateExamSession(id, request);
    ApiResponse<ExamSessionResponse> response =
        ApiResponse.success("EXAM_SESSION_UPDATED_SUCCESSFULLY", updatedSession);
    return ResponseEntity.ok(response);
  }
}
