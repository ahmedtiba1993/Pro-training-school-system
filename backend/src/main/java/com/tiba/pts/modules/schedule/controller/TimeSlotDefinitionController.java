package com.tiba.pts.modules.schedule.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.schedule.dto.request.TimeSlotDefinitionRequest;
import com.tiba.pts.modules.schedule.dto.response.TimeSlotDefinitionResponse;
import com.tiba.pts.modules.schedule.service.TimeSlotDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-slots")
@RequiredArgsConstructor
@Validated
public class TimeSlotDefinitionController {

  private final TimeSlotDefinitionService service;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<TimeSlotDefinitionResponse>>> getAllTimeSlots() {
    List<TimeSlotDefinitionResponse> data = service.getAllTimeSlots();
    return ResponseEntity.ok(ApiResponse.success("TIME_SLOTS_RETRIEVED_SUCCESSFULLY", data));
  }

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTimeSlot(
      @Valid @RequestBody TimeSlotDefinitionRequest request) {
    Long createdId = service.createTimeSlot(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("TIME_SLOT_CREATED_SUCCESSFULLY", createdId));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateTimeSlot(
      @PathVariable Long id, @Valid @RequestBody TimeSlotDefinitionRequest request) {
    Long updatedId = service.updateTimeSlot(id, request);
    return ResponseEntity.ok(ApiResponse.success("TIME_SLOT_UPDATED_SUCCESSFULLY", updatedId));
  }
}
