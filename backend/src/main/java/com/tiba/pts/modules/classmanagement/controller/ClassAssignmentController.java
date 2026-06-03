package com.tiba.pts.modules.classmanagement.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.classmanagement.dto.request.ClassAssignmentRequest;
import com.tiba.pts.modules.classmanagement.dto.response.ClassStudentResponse;
import com.tiba.pts.modules.classmanagement.service.ClassAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/class-assignments")
@RequiredArgsConstructor
public class ClassAssignmentController {

  private final ClassAssignmentService classAssignmentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> assignStudentToClass(
      @Valid @RequestBody ClassAssignmentRequest request) {

    ApiResponse<Long> response =
        ApiResponse.success(
            "STUDENT_ASSIGNED_TO_CLASS_SUCCESSFULLY",
            classAssignmentService.assignStudent(request));

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/class-groups/{classGroupId}/students")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ClassStudentResponse>>> getStudentsByClass(
      @PathVariable Long classGroupId) {

    List<ClassStudentResponse> students =
        classAssignmentService.getStudentsByClassGroup(classGroupId);

    ApiResponse<List<ClassStudentResponse>> response =
        ApiResponse.success("CLASS_STUDENT_LIST_RETRIEVED", students);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/class-groups/{classGroupId}/active-students")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ClassStudentResponse>>> getActiveStudentsByClass(
      @PathVariable Long classGroupId) {

    List<ClassStudentResponse> students =
        classAssignmentService.getActiveStudentsByClassGroup(classGroupId);

    ApiResponse<List<ClassStudentResponse>> response =
        ApiResponse.success("ACTIVE_CLASS_STUDENT_LIST_RETRIEVED", students);

    return ResponseEntity.ok(response);
  }
}
