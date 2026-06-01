package com.tiba.pts.modules.classmanagement.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import com.tiba.pts.modules.classmanagement.dto.request.ClassGroupRequest;
import com.tiba.pts.modules.classmanagement.dto.response.ActiveClassGroupResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassGroupDetailResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassGroupResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassManagementStatsResponse;
import com.tiba.pts.modules.classmanagement.service.ClassGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;

@RestController
@RequestMapping("/api/v1/class-groups")
@RequiredArgsConstructor
public class ClassGroupController {

  private final ClassGroupService classGroupService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createClassGroup(
      @Valid @RequestBody ClassGroupRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "CLASS_GROUP_CREATED_SUCCESSFULLY", classGroupService.createClassGroup(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ClassGroupResponse>>> getAllClassGroups(
      @RequestParam(required = false) Long levelId,
      @RequestParam(required = false) Long trainingId,
      @RequestParam(required = false) ClassStatus status) {

    List<ClassGroupResponse> listData =
        classGroupService.getAllFiltered(levelId, trainingId, status);

    ApiResponse<List<ClassGroupResponse>> response =
        ApiResponse.success("CLASS_GROUP_LIST_RETRIEVED", listData);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ClassManagementStatsResponse>> getClassManagementStats() {

    ApiResponse<ClassManagementStatsResponse> response =
        ApiResponse.success(
            "CLASS_MANAGEMENT_STATS_RETRIEVED", classGroupService.getClassManagementStats());

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateClassGroupStatus(
      @PathVariable Long id, @RequestParam(name = "status", required = true) ClassStatus status) {

    ApiResponse<Long> response =
        ApiResponse.success(
            "CLASS_GROUP_STATUS_UPDATED_SUCCESSFULLY", classGroupService.updateStatus(id, status));

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/details")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ClassGroupDetailResponse>> getClassGroupDetails(
      @PathVariable Long id) {

    ClassGroupDetailResponse detail = classGroupService.getDetailById(id);

    ApiResponse<ClassGroupDetailResponse> response =
        ApiResponse.success("CLASS_GROUP_DETAILS_RETRIEVED", detail);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/export/pdf")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {

    // Retrieve the binary stream of the PDF via your service
    byte[] pdfBytes = classGroupService.exportStudentsPdf(id);

    // Configuration of HTTP headers in "Form Data" style
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "liste_eleves_classe_" + id + ".pdf");

    // Return of the binary response
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  @GetMapping("/{id}/export/pdf/documents")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> exportPdfWithDocuments(
      @PathVariable Long id, @RequestParam List<Long> documentIds) {

    // Call the service to get the binary PDF
    byte[] pdfBytes = classGroupService.exportStudentsWithDocsPdf(id, documentIds);

    // Strict configuration of direct download HTTP headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "liste_eleves_documents_" + id + ".pdf");

    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ActiveClassGroupResponse>>> getAllActiveClasses() {
    List<ActiveClassGroupResponse> activeClasses = classGroupService.getAllActiveClasses();
    ApiResponse<List<ActiveClassGroupResponse>> response =
        ApiResponse.success("ACTIVE_CLASS_LIST_RETRIEVED", activeClasses);
    return ResponseEntity.ok(response);
  }
}
