package com.tiba.pts.modules.examscheduling.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.examscheduling.dto.request.ExamTimeSlotRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamTimeSlotResponse;
import com.tiba.pts.modules.examscheduling.service.ExamTimeSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-time-slots")
@RequiredArgsConstructor
@Validated
public class ExamTimeSlotController {

  private final ExamTimeSlotService examTimeSlotService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createExamTimeSlot(
      @Valid @RequestBody ExamTimeSlotRequest request) {
    Long id = examTimeSlotService.createExamTimeSlot(request);
    ApiResponse<Long> response = ApiResponse.success("EXAM_TIME_SLOT_CREATED_SUCCESSFULLY", id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<ExamTimeSlotResponse>>> getAllExamTimeSlots() {
    List<ExamTimeSlotResponse> slots = examTimeSlotService.getAllExamTimeSlots();
    ApiResponse<List<ExamTimeSlotResponse>> response =
        ApiResponse.success("EXAM_TIME_SLOT_LIST_RETRIEVED", slots);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ExamTimeSlotResponse>> getExamTimeSlotById(
      @PathVariable Long id) {
    ExamTimeSlotResponse slot = examTimeSlotService.getExamTimeSlotById(id);
    ApiResponse<ExamTimeSlotResponse> response =
        ApiResponse.success("EXAM_TIME_SLOT_RETRIEVED", slot);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/toggle")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> toggleActiveStatus(@PathVariable Long id) {
    examTimeSlotService.toggleActiveStatus(id);
    return ResponseEntity.ok(ApiResponse.success("EXAM_TIME_SLOT_STATUS_TOGGLED_SUCCESSFULLY"));
  }

  @GetMapping("/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<ExamTimeSlotResponse>>> getActiveExamTimeSlots() {
    List<ExamTimeSlotResponse> slots = examTimeSlotService.getActiveExamTimeSlots();
    ApiResponse<List<ExamTimeSlotResponse>> response =
        ApiResponse.success("ACTIVE_EXAM_TIME_SLOT_LIST_RETRIEVED", slots);
    return ResponseEntity.ok(response);
  }
}
