package com.tiba.pts.modules.specialty.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.specialty.dto.LevelDto;
import com.tiba.pts.modules.specialty.service.LevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
@Validated
public class LevelController {

  private final LevelService levelService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createLevel(@Valid @RequestBody LevelDto request) {
    ApiResponse<Long> response =
        ApiResponse.success("LEVEL_CREATED_SUCCESSFULLY", levelService.create(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<LevelDto>>> getAllLevels() {
    List<LevelDto> data = levelService.getAll();
    ApiResponse<List<LevelDto>> response = ApiResponse.success("LEVEL_LIST_RETRIEVED", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateLevel(
      @PathVariable Long id, @Valid @RequestBody LevelDto request) {
    Long updatedId = levelService.update(id, request);
    ApiResponse<Long> response = ApiResponse.success("LEVEL_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }
}
