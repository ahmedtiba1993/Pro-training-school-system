package com.tiba.pts.modules.schedule.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.schedule.dto.request.TimetableSlotRequest;
import com.tiba.pts.modules.schedule.dto.response.TimetableSlotDetailResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableTeacherViewResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableViewResponse;
import com.tiba.pts.modules.schedule.service.TimetableSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/timetable-slots")
@RequiredArgsConstructor
public class TimetableSlotController {

  private final TimetableSlotService timetableSlotService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createSlot(
      @Valid @RequestBody TimetableSlotRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "TIMETABLE_SLOT_CREATED_SUCCESSFULLY", timetableSlotService.addTimetableSlot(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateSlot(
      @PathVariable Long id, @Valid @RequestBody TimetableSlotRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "TIMETABLE_SLOT_UPDATED_SUCCESSFULLY", timetableSlotService.updateTimetableSlot(id, request));
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/view/{scheduleId}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<TimetableViewResponse>> getTimetableView(
      @PathVariable Long scheduleId) {
    TimetableViewResponse viewResponse = timetableSlotService.getTimetableView(scheduleId);
    ApiResponse<TimetableViewResponse> response =
        ApiResponse.success("TIMETABLE_VIEW_RETRIEVED_SUCCESSFULLY", viewResponse);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/view/teacher/{teacherId}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<TimetableTeacherViewResponse>> getTimetableViewByTeacher(
      @PathVariable Long teacherId) {
    TimetableTeacherViewResponse viewResponse = timetableSlotService.getTimetableViewByTeacher(teacherId);
    ApiResponse<TimetableTeacherViewResponse> response =
        ApiResponse.success("TIMETABLE_VIEW_RETRIEVED_SUCCESSFULLY", viewResponse);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<ApiResponse<TimetableSlotDetailResponse>> getSlotById(
      @PathVariable Long id) {
    TimetableSlotDetailResponse detail = timetableSlotService.getById(id);
    ApiResponse<TimetableSlotDetailResponse> response =
        ApiResponse.success("TIMETABLE_SLOT_RETRIEVED_SUCCESSFULLY", detail);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/view/{scheduleId}/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> exportTimetablePdfBySchedule(
      @PathVariable Long scheduleId) {
    byte[] pdfBytes = timetableSlotService.exportTimetablePdfBySchedule(scheduleId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "emploi_du_temps_schedule_" + scheduleId + ".pdf");
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  @GetMapping("/view/teacher/{teacherId}/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> exportTimetablePdfByTeacher(
      @PathVariable Long teacherId) {
    byte[] pdfBytes = timetableSlotService.exportTimetablePdfByTeacher(teacherId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "emploi_du_temps_enseignant_" + teacherId + ".pdf");
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }
}
