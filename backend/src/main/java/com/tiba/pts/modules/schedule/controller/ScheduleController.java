package com.tiba.pts.modules.schedule.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import com.tiba.pts.modules.schedule.dto.request.ScheduleRequest;
import com.tiba.pts.modules.schedule.dto.request.ScheduleStatusRequest;
import com.tiba.pts.modules.schedule.dto.request.ScheduleUpdateRequest;
import com.tiba.pts.modules.schedule.dto.response.ScheduleResponse;
import com.tiba.pts.modules.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

  private final ScheduleService scheduleService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createSchedule(
      @Valid @RequestBody ScheduleRequest request) {

    ApiResponse<Long> response =
        ApiResponse.success(
            "SCHEDULE_CREATED_SUCCESSFULLY", scheduleService.createSchedule(request));

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/status/{status}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> findAllByStatus(
      @PathVariable ScheduleStatus status) {

    ApiResponse<List<ScheduleResponse>> response =
        ApiResponse.success(
            "SCHEDULES_FETCHED_SUCCESSFULLY", scheduleService.findAllByStatus(status));

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
      @PathVariable Long id, @Valid @RequestBody ScheduleUpdateRequest request) {

    ApiResponse<ScheduleResponse> response =
        ApiResponse.success(
            "SCHEDULE_UPDATED_SUCCESSFULLY", scheduleService.updateSchedule(id, request));

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ScheduleResponse>> changeScheduleStatus(
      @PathVariable Long id, @Valid @RequestBody ScheduleStatusRequest request) {

    ApiResponse<ScheduleResponse> response =
        ApiResponse.success(
            "SCHEDULE_STATUS_CHANGED_SUCCESSFULLY", scheduleService.changeStatus(id, request));

    return ResponseEntity.ok(response);
  }
}
