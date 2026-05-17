package com.tiba.pts.modules.profiles.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse; // <-- L'import de ton DTO
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import com.tiba.pts.modules.profiles.dto.request.TeacherFiltreRequest;
import com.tiba.pts.modules.profiles.dto.request.TeacherRequest;
import com.tiba.pts.modules.profiles.dto.response.TeacherResponse;
import com.tiba.pts.modules.profiles.service.TeacherService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Validated
public class TeacherController {

  private final TeacherService teacherService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTeacher(
      @Valid @RequestBody TeacherRequest request) {
    Long teacherId = teacherService.createTeacher(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("TEACHER_CREATED_SUCCESSFULLY", teacherId));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<TeacherResponse>>> getAllTeachers(
      TeacherFiltreRequest filtre,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

    PageResponse<TeacherResponse> paginatedData = teacherService.getAllPaged(filtre, page, size);

    return ResponseEntity.ok(ApiResponse.success("TEACHERS_RETRIEVED_SUCCESSFULLY", paginatedData));
  }

  @GetMapping("/count/active")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> getActiveTeachersCount() {
    long count = teacherService.countActiveTeachers();
    return ResponseEntity.ok(
        ApiResponse.success("ACTIVE_TEACHERS_COUNT_RETRIEVED_SUCCESSFULLY", count));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<TeacherResponse>> getTeacherById(@PathVariable Long id) {

    TeacherResponse response = teacherService.getTeacherById(id);

    return ResponseEntity.ok(ApiResponse.success("TEACHER_RETRIEVED_SUCCESSFULLY", response));
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTeacherStatus(
      @PathVariable @NotNull Long id, @RequestParam @NotNull TeacherStatus status) {

    Long updatedId = teacherService.updateTeacherStatus(id, status);
    return ResponseEntity.ok(ApiResponse.success("TEACHER_STATUS_UPDATED_SUCCESSFULLY", updatedId));
  }
}
