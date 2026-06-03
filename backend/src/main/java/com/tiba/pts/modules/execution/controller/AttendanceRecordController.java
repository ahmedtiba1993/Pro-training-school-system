package com.tiba.pts.modules.execution.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.execution.dto.request.AttendanceRequest;
import com.tiba.pts.modules.execution.dto.response.AttendanceRecordResponse;
import com.tiba.pts.modules.execution.dto.response.AttendanceStatsResponse;
import com.tiba.pts.modules.execution.dto.response.StudentAttendanceResponse;
import com.tiba.pts.modules.execution.service.AttendanceRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
@Validated
public class AttendanceRecordController {

  private final AttendanceRecordService attendanceRecordService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> submitAttendance(
      @Valid @RequestBody AttendanceRequest request) {
    Long data = attendanceRecordService.submitAttendance(request);
    ApiResponse<Long> response = ApiResponse.success("ATTENDANCE_SUBMITTED_SUCCESSFULLY", data);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/session/{courseSessionId}/stats")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<AttendanceStatsResponse>> getAttendanceStats(
      @PathVariable Long courseSessionId) {
    AttendanceStatsResponse data = attendanceRecordService.getAttendanceStats(courseSessionId);
    ApiResponse<AttendanceStatsResponse> response =
        ApiResponse.success("ATTENDANCE_STATS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/session/{courseSessionId}/students")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<StudentAttendanceResponse>>> getStudentsAttendanceForSession(
      @PathVariable Long courseSessionId) {
    List<StudentAttendanceResponse> data = attendanceRecordService.getStudentsAttendanceForSession(courseSessionId);
    ApiResponse<List<StudentAttendanceResponse>> response =
        ApiResponse.success("SESSION_STUDENT_ATTENDANCE_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }
}

