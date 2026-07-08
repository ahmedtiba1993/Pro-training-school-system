package com.tiba.pts.modules.examscheduling.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.examscheduling.dto.request.ExamRoomAllocationRequest;
import com.tiba.pts.modules.examscheduling.service.ExamRoomAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ExamRoomAllocationController {

  private final ExamRoomAllocationService examRoomAllocationService;

  @PostMapping("/exam-schedules/{scheduleId}/rooms")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> allocateRoom(
      @PathVariable Long scheduleId, @Valid @RequestBody ExamRoomAllocationRequest request) {
    Long id = examRoomAllocationService.allocateRoom(scheduleId, request);
    ApiResponse<Long> response = ApiResponse.success("ROOM_ALLOCATED_SUCCESSFULLY", id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @DeleteMapping("/exam-room-allocations/{allocationId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deallocateRoom(@PathVariable Long allocationId) {
    examRoomAllocationService.deallocateRoom(allocationId);
    ApiResponse<Void> response = ApiResponse.success("ROOM_DEALLOCATED_SUCCESSFULLY");
    return ResponseEntity.ok(response);
  }
}
