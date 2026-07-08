package com.tiba.pts.modules.examscheduling.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.examscheduling.dto.request.ExamScheduleRequest;
import com.tiba.pts.modules.examscheduling.service.ExamScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tiba.pts.modules.examscheduling.dto.response.ExamScheduleResponse;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ExamScheduleController {

  private final ExamScheduleService examScheduleService;

  @PostMapping("/exam-timetables/{timetableId}/schedules")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createSchedule(
      @PathVariable Long timetableId, @Valid @RequestBody ExamScheduleRequest request) {
    Long id = examScheduleService.createSchedule(timetableId, request);
    ApiResponse<Long> response = ApiResponse.success("EXAM_SCHEDULE_CREATED_SUCCESSFULLY", id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping(value = {"/exam-schedules/{scheduleId}", "/exam-timetables/schedules/{scheduleId}"})
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateSchedule(
      @PathVariable Long scheduleId, @Valid @RequestBody ExamScheduleRequest request) {
    Long id = examScheduleService.updateSchedule(scheduleId, request);
    ApiResponse<Long> response = ApiResponse.success("EXAM_SCHEDULE_UPDATED_SUCCESSFULLY", id);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping(value = {"/exam-schedules/{scheduleId}", "/exam-timetables/schedules/{scheduleId}"})
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> removeSchedule(@PathVariable Long scheduleId) {
    examScheduleService.removeSchedule(scheduleId);
    ApiResponse<Void> response = ApiResponse.success("EXAM_SCHEDULE_DELETED_SUCCESSFULLY");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/exam-timetables/{timetableId}/schedules")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<java.util.List<ExamScheduleResponse>>> getSchedulesByTimetableId(
      @PathVariable Long timetableId) {
    java.util.List<ExamScheduleResponse> schedules =
        examScheduleService.getSchedulesByTimetableId(timetableId);
    ApiResponse<java.util.List<ExamScheduleResponse>> response =
        ApiResponse.success("EXAM_SCHEDULES_RETRIEVED", schedules);
    return ResponseEntity.ok(response);
  }
}
