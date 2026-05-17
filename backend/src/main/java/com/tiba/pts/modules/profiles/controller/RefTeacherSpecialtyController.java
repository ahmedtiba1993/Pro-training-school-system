package com.tiba.pts.modules.profiles.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.profiles.dto.request.RefTeacherSpecialtyRequest;
import com.tiba.pts.modules.profiles.dto.request.UpdateSpecialtyRequest;
import com.tiba.pts.modules.profiles.dto.response.RefTeacherSpecialtyResponse;
import com.tiba.pts.modules.profiles.dto.response.RefTeacherSpecialtySimpleResponse;
import com.tiba.pts.modules.profiles.dto.response.TeacherSimpleResponse;
import com.tiba.pts.modules.profiles.service.RefTeacherSpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-specialties")
@RequiredArgsConstructor
public class RefTeacherSpecialtyController {

  private final RefTeacherSpecialtyService specialtyService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createTeacherSpecialty(
      @Valid @RequestBody RefTeacherSpecialtyRequest request) {
    Long response = specialtyService.createSpecialty(request);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_CREATED_SUCCESSFULLY", response));
  }

  // GET ALL WITHOUT COUNT (Used for selectors/dropdowns)
  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<RefTeacherSpecialtySimpleResponse>>>
      getAllTeacherSpecialtiesSimple() {
    List<RefTeacherSpecialtySimpleResponse> response = specialtyService.getAllSpecialtiesSimple();
    return ResponseEntity.ok(ApiResponse.success("SPECIALTIES_RETRIEVED_SUCCESSFULLY", response));
  }

  @GetMapping("/with-count")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<RefTeacherSpecialtyResponse>>>
      getAllTeacherSpecialtiesWithCount(
          @RequestParam(required = false, name = "search") String search) {

    List<RefTeacherSpecialtyResponse> response =
        specialtyService.getAllSpecialtiesWithCount(search);

    return ResponseEntity.ok(
        ApiResponse.success("SPECIALTIES_WITH_COUNT_RETRIEVED_SUCCESSFULLY", response));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<RefTeacherSpecialtyResponse>> getTeacherSpecialty(
      @PathVariable Long id) {
    RefTeacherSpecialtyResponse response = specialtyService.getSpecialtyById(id);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_RETRIEVED_SUCCESSFULLY", response));
  }

  @PutMapping("/{id}/label")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateRefTeacherSpeciality(
      @PathVariable Long id, @Valid @RequestBody UpdateSpecialtyRequest request) {
    Long response = specialtyService.update(id, request);
    return ResponseEntity.ok(ApiResponse.success("SPECIALTY_LABEL_UPDATED_SUCCESSFULLY", response));
  }

  @GetMapping("/{id}/active-teachers")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<TeacherSimpleResponse>>> getActiveTeachersBySpecialty(
      @PathVariable Long id) {

    List<TeacherSimpleResponse> response = specialtyService.getActiveTeachersBySpecialty(id);

    return ResponseEntity.ok(
        ApiResponse.success("ACTIVE_TEACHERS_RETRIEVED_SUCCESSFULLY", response));
  }
}
