package com.tiba.pts.modules.subject.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.subject.domain.enums.SubjectStatus;
import com.tiba.pts.modules.subject.dto.request.SubjectRequest;
import com.tiba.pts.modules.subject.dto.response.SubjectResponse;
import com.tiba.pts.modules.subject.dto.response.SubjectShortResponse;
import com.tiba.pts.modules.subject.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Validated
public class SubjectController {

  private final SubjectService subjectService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createSubject(
      @Valid @RequestBody SubjectRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success("SUBJECT_CREATED_SUCCESSFULLY", subjectService.createSubject(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<SubjectResponse>>> getAllSubjects() {
    List<SubjectResponse> data = subjectService.getAllSubjects();
    return ResponseEntity.ok(ApiResponse.success("SUBJECT_LIST_RETRIEVED", data));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<SubjectResponse>> getSubjectById(@PathVariable Long id) {
    SubjectResponse data = subjectService.getSubjectById(id);
    ApiResponse<SubjectResponse> response = ApiResponse.success("SUBJECT_FOUND", data);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateSubject(
      @PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
    Long updatedId = subjectService.updateSubject(id, request);
    ApiResponse<Long> response = ApiResponse.success("SUBJECT_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changeSubjectStatus(
      @PathVariable Long id, @RequestParam SubjectStatus newStatus) {
    subjectService.changeStatus(id, newStatus);
    return ResponseEntity.ok(ApiResponse.success("SUBJECT_STATUS_CHANGED"));
  }

  @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> uploadSubjectPdf(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {

    Long updatedId = subjectService.uploadPdf(id, file);

    return ResponseEntity.ok(ApiResponse.success("PDF_UPLOADED_SUCCESSFULLY", updatedId));
  }

  @GetMapping("/{id}/pdf")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<Resource> getSubjectPdf(@PathVariable Long id) {
    Resource resource = subjectService.getPdfAsResource(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        // "inline" allows the browser to display the PDF directly instead of forcing download
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }

  @GetMapping("/promotion/{promotionId}/catalog-subjects")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SECRETARIAT', 'ROLE_FORMATEUR', 'ROLE_APPRENANT')")
  public ResponseEntity<ApiResponse<List<SubjectShortResponse>>> getCatalogSubjectsByPromotionId(
      @PathVariable Long promotionId) {

    List<SubjectShortResponse> data =
        subjectService.getCatalogSubjectsShortByPromotion(promotionId);

    ApiResponse<List<SubjectShortResponse>> response =
        ApiResponse.success("CATALOG_SUBJECTS_RETRIEVED_SUCCESSFULLY", data);

    return ResponseEntity.ok(response);
  }
}
