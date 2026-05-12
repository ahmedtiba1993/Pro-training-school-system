package com.tiba.pts.modules.documents.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.documents.dto.request.EnrollmentDocumentRequest;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentResponse;
import com.tiba.pts.modules.documents.service.EnrollmentDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollment-documents")
@RequiredArgsConstructor
@Validated
public class EnrollmentDocumentController {

  private final EnrollmentDocumentService documentService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createDocument(
      @Valid @RequestBody EnrollmentDocumentRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "REGISTRATION_DOCUMENT_CREATED_SUCCESSFULLY", documentService.addDocument(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<EnrollmentDocumentResponse>>> getAllDocuments() {
    List<EnrollmentDocumentResponse> dataList = documentService.getAll();
    ApiResponse<List<EnrollmentDocumentResponse>> response =
        ApiResponse.success("REGISTRATION_DOCUMENT_LIST_RETRIEVED", dataList);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateDocument(
      @PathVariable Long id, @Valid @RequestBody EnrollmentDocumentRequest request) {
    Long updatedData = documentService.update(id, request);
    ApiResponse<Long> response =
        ApiResponse.success("REGISTRATION_DOCUMENT_UPDATED_SUCCESSFULLY", updatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/level/{levelId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<EnrollmentDocumentResponse>>> getDocumentsByLevel(
      @PathVariable Long levelId) {
    List<EnrollmentDocumentResponse> documents = documentService.getDocumentsByLevelId(levelId);
    ApiResponse<List<EnrollmentDocumentResponse>> response =
        ApiResponse.success("REGISTRATION_DOCUMENTS_BY_LEVEL_RETRIEVED", documents);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/export/arabic-pdf")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public ResponseEntity<byte[]> exportArabicForm() {
    byte[] pdfBytes = documentService.exportArabicEnrollmentForm();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDispositionFormData("attachment", "fiche_inscription.pdf");
    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }
}
