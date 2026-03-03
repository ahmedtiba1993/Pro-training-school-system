package com.tiba.pts.modules.academicyear.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.academicyear.dto.TermDto;
import com.tiba.pts.modules.academicyear.service.TermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

  private final TermService termService;

  @PostMapping
  public ResponseEntity<ApiResponse<Long>> createTerm(@Valid @RequestBody TermDto request) {
    Long createdId = termService.create(request);
    ApiResponse<Long> response = ApiResponse.success("TERM_CREATED", createdId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<TermDto>>> getAllTermsByYear(
      @RequestParam Long academicYearId) {
    List<TermDto> termResponses = termService.getAllByAcademicYear(academicYearId);
    ApiResponse<List<TermDto>> response = ApiResponse.success("TERMS_RETRIEVED", termResponses);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TermDto>> updateTerm(
      @PathVariable Long id, @Valid @RequestBody TermDto request) {
    TermDto data = termService.update(id, request);
    ApiResponse<TermDto> response = ApiResponse.success("TERM_UPDATED", data);
    return ResponseEntity.ok(response);
  }
}
