package com.tiba.pts.modules.documents.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.documents.dto.request.EnrollmentDocumentRequest;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentResponse;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentSimpleResponse;
import com.tiba.pts.modules.documents.service.EnrollmentDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollment-documents")
@RequiredArgsConstructor
@Validated
public class EnrollmentDocumentController {

  private final EnrollmentDocumentService documentService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createDocument(
      @Valid @RequestBody EnrollmentDocumentRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "DOCUMENTS_ENROLLMENT_DOCUMENT_CREATED_SUCCESSFULLY",
            documentService.addDocument(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<EnrollmentDocumentResponse>>> getAllDocuments() {
    List<EnrollmentDocumentResponse> dataList = documentService.getAll();
    ApiResponse<List<EnrollmentDocumentResponse>> response =
        ApiResponse.success("DOCUMENTS_ENROLLMENT_DOCUMENT_LIST_RETRIEVED", dataList);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<EnrollmentDocumentResponse>>> getActiveDocuments() {
    List<EnrollmentDocumentResponse> dataList = documentService.getAllActive();
    ApiResponse<List<EnrollmentDocumentResponse>> response =
        ApiResponse.success("DOCUMENTS_ENROLLMENT_DOCUMENT_ACTIVE_LIST_RETRIEVED", dataList);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateDocumentStatus(
      @PathVariable Long id, @RequestParam boolean isActive) {
    Long updatedId = documentService.updateStatus(id, isActive);
    ApiResponse<Long> response =
        ApiResponse.success(
            "DOCUMENTS_ENROLLMENT_DOCUMENT_STATUS_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateDocument(
      @PathVariable Long id, @Valid @RequestBody EnrollmentDocumentRequest request) {
    Long updatedData = documentService.update(id, request);
    ApiResponse<Long> response =
        ApiResponse.success("DOCUMENTS_ENROLLMENT_DOCUMENT_UPDATED_SUCCESSFULLY", updatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/level/{levelId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<EnrollmentDocumentResponse>>> getDocumentsByLevel(
      @PathVariable Long levelId) {
    List<EnrollmentDocumentResponse> documents = documentService.getDocumentsByLevelId(levelId);
    ApiResponse<List<EnrollmentDocumentResponse>> response =
        ApiResponse.success("DOCUMENTS_ENROLLMENT_DOCUMENTS_BY_LEVEL_RETRIEVED", documents);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/export/arabic-pdf")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> exportArabicForm(@RequestParam(required = false) Long levelId) {
    byte[] pdfBytes = documentService.exportArabicEnrollmentForm(levelId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename("fiche_inscription.pdf", StandardCharsets.UTF_8)
            .build());
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  @GetMapping("/class-group/{classGroupId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<EnrollmentDocumentSimpleResponse>>>
      getDocumentsByClassGroup(@PathVariable Long classGroupId) {

    List<EnrollmentDocumentSimpleResponse> documents =
        documentService.getDocumentsByClassGroupId(classGroupId);

    ApiResponse<List<EnrollmentDocumentSimpleResponse>> response =
        ApiResponse.success("DOCUMENTS_ENROLLMENT_DOCUMENTS_BY_CLASS_GROUP_RETRIEVED", documents);

    return ResponseEntity.ok(response);
  }
}
