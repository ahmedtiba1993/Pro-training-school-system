package com.tiba.pts.modules.profiles.controller;

import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.response.StudentResponse;
import com.tiba.pts.modules.profiles.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
