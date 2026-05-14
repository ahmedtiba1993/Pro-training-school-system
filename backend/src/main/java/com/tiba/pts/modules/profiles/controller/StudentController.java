package com.tiba.pts.modules.profiles.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.profiles.dto.request.ExistenceCheckResponse;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.response.StudentResponse;
import com.tiba.pts.modules.profiles.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles/students")
@RequiredArgsConstructor
public class StudentController {

  private final StudentService studentService;

  @PostMapping
  public ResponseEntity<Long> createStudent(@Valid @RequestBody StudentRequest request) {
    Long response = studentService.createStudent(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @GetMapping("/{id}")
  public ResponseEntity<StudentResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(studentService.getStudentById(id));
  }

  @GetMapping("/search")
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
}
