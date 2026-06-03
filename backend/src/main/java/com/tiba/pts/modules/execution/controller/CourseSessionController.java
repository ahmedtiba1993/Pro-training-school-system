package com.tiba.pts.modules.execution.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.execution.dto.request.CourseSessionRequest;
import com.tiba.pts.modules.execution.dto.request.CourseSessionSearchRequest;
import com.tiba.pts.modules.execution.dto.request.CourseSessionUpdateRequest;
import com.tiba.pts.modules.execution.dto.request.ForceCompleteCommand;
import com.tiba.pts.modules.execution.dto.response.CourseSessionResponse;
import com.tiba.pts.modules.execution.dto.response.CourseSessionStatsResponse;
import com.tiba.pts.modules.execution.service.CourseSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/course-sessions")
@RequiredArgsConstructor
@Validated
public class CourseSessionController {

  private final CourseSessionService courseSessionService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createCourseSession(
      @Valid @RequestBody CourseSessionRequest request) {
    Long data = courseSessionService.createCourseSession(request);
    ApiResponse<Long> response = ApiResponse.success("COURSE_SESSION_CREATED_SUCCESSFULLY", data);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionResponse>> updateCourseSession(
      @PathVariable Long id, @Valid @RequestBody CourseSessionUpdateRequest request) {
    CourseSessionResponse data = courseSessionService.updateCourseSession(id, request);
    ApiResponse<CourseSessionResponse> response =
        ApiResponse.success("COURSE_SESSION_UPDATED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<com.tiba.pts.core.dto.PageResponse<CourseSessionResponse>>>
      getAllCourseSessions(
          @ModelAttribute CourseSessionSearchRequest filterRequest,
          org.springframework.data.domain.Pageable pageable) {
    com.tiba.pts.core.dto.PageResponse<CourseSessionResponse> data =
        courseSessionService.getCourseSessionsPaged(filterRequest, pageable);
    ApiResponse<com.tiba.pts.core.dto.PageResponse<CourseSessionResponse>> response =
        ApiResponse.success("COURSE_SESSIONS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionResponse>> cancelSession(
      @PathVariable Long id, @RequestParam String reason) {
    CourseSessionResponse data = courseSessionService.cancelSession(id, reason);
    ApiResponse<CourseSessionResponse> response =
        ApiResponse.success("COURSE_SESSION_CANCELLED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/reopen")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionResponse>> reopenSession(@PathVariable Long id) {
    CourseSessionResponse data = courseSessionService.reopenSession(id);
    ApiResponse<CourseSessionResponse> response =
        ApiResponse.success("COURSE_SESSION_REOPENED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/force-complete")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionResponse>> forceCompleteSession(
      @PathVariable Long id, @Valid @RequestBody ForceCompleteCommand cmd) {
    CourseSessionResponse data = courseSessionService.forceCompleteSession(id, cmd);
    ApiResponse<CourseSessionResponse> response =
        ApiResponse.success("COURSE_SESSION_FORCE_COMPLETED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/restore")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionResponse>> restoreCanceledSession(
      @PathVariable Long id) {
    CourseSessionResponse data = courseSessionService.restoreCanceledSession(id);
    ApiResponse<CourseSessionResponse> response =
        ApiResponse.success("COURSE_SESSION_RESTORED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionResponse>> getCourseSessionById(
      @PathVariable Long id) {
    CourseSessionResponse data = courseSessionService.getCourseSessionById(id);
    ApiResponse<CourseSessionResponse> response =
        ApiResponse.success("COURSE_SESSION_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/today")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<CourseSessionStatsResponse>> getTodayStats() {
    CourseSessionStatsResponse data = courseSessionService.getTodayStats();
    ApiResponse<CourseSessionStatsResponse> response =
        ApiResponse.success("COURSE_SESSION_STATS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }
}
