package com.tiba.pts.modules.profiles.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.profiles.domain.enums.StudentStatus;
import com.tiba.pts.modules.profiles.dto.request.ExistenceCheckResponse;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.response.StudentListResponse;
import com.tiba.pts.modules.profiles.dto.response.StudentResponse;
import com.tiba.pts.modules.profiles.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles/students")
@RequiredArgsConstructor
public class StudentController {

  private final StudentService studentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<Long> createStudent(@Valid @RequestBody StudentRequest request) {
    Long response = studentService.createStudent(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<StudentResponse>> getStudentById(@PathVariable Long id) {
    StudentResponse student = studentService.getStudentById(id);
    ApiResponse<StudentResponse> response =
        ApiResponse.success("STUDENT_RETRIEVED_SUCCESSFULLY", student);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<StudentResponse>>> searchStudents(
      @RequestParam(required = false, name = "keyword") String keyword) {
    List<StudentResponse> students = studentService.searchStudents(keyword);
    ApiResponse<List<StudentResponse>> apiResponse =
        ApiResponse.<List<StudentResponse>>builder()
            .success(true)
            .message("STUDENTS_RETRIEVED_SUCCESSFULLY")
            .data(students)
            .build();
    return ResponseEntity.ok(apiResponse);
  }

  @GetMapping("/check-existence")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ExistenceCheckResponse>> checkExistence(
      @RequestParam(required = false) String cin,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phone) {

    ExistenceCheckResponse existenceData = studentService.checkExistence(cin, email, phone);

    String message = existenceData.hasAnyConflict() ? "CONFLICTS_FOUND" : "NO_CONFLICTS_FOUND";

    ApiResponse<ExistenceCheckResponse> apiResponse =
        ApiResponse.<ExistenceCheckResponse>builder()
            .success(true)
            .message(message)
            .data(existenceData)
            .build();

    return ResponseEntity.ok(apiResponse);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<StudentListResponse>>> getStudentsPaginated(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) StudentStatus status) {

    PageResponse<StudentListResponse> paginatedStudents =
        studentService.getStudentsPaginated(page, size, keyword, status);
    ApiResponse<PageResponse<StudentListResponse>> response =
        ApiResponse.<PageResponse<StudentListResponse>>builder()
            .success(true)
            .message("STUDENT_LIST_RETRIEVED_SUCCESSFULLY")
            .data(paginatedStudents)
            .build();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/count/active")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> countActiveStudents() {
    Long activeCount = studentService.countActiveStudents();
    ApiResponse<Long> apiResponse =
        ApiResponse.<Long>builder()
            .success(true)
            .message("ACTIVE_STUDENTS_COUNT_RETRIEVED_SUCCESSFULLY")
            .data(activeCount)
            .build();

    return ResponseEntity.ok(apiResponse);
  }
}
