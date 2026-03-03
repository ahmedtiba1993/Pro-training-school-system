package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.academicyear.dto.ExamSessionDto;
import com.tiba.pts.modules.academicyear.service.ExamSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-sessions")
@RequiredArgsConstructor
public class ExamSessionController {

  private final ExamSessionService examSessionService;

  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createExamSession(
      @Valid @RequestBody ExamSessionDto request) {
    Long createdId = examSessionService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("EXAM_SESSION_CREATED", createdId));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<ExamSessionDto>>> getAllByTerm(Long termId) {
    List<ExamSessionDto> sessions = examSessionService.getAllByTerm(termId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success("EXAM_SESSION_LIST", sessions));
  }
}
