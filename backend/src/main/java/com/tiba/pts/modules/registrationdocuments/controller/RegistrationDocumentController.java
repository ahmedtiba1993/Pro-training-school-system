package com.tiba.pts.modules.registrationdocuments.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.registrationdocuments.dto.RegistrationDocumentRequest;
import com.tiba.pts.modules.registrationdocuments.dto.RegistrationDocumentResponse;
import com.tiba.pts.modules.registrationdocuments.service.RegistrationDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registration-documents")
@RequiredArgsConstructor
@Validated
public class RegistrationDocumentController {

  private final RegistrationDocumentService documentService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createDocument(
      @Valid @RequestBody RegistrationDocumentRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success(
            "REGISTRATION_DOCUMENT_CREATED_SUCCESSFULLY", documentService.addDocument(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<RegistrationDocumentResponse>>> getAllDocuments() {
    List<RegistrationDocumentResponse> dataList = documentService.getAll();
    ApiResponse<List<RegistrationDocumentResponse>> response =
        ApiResponse.success("REGISTRATION_DOCUMENT_LIST_RETRIEVED", dataList);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateDocument(
      @PathVariable Long id, @Valid @RequestBody RegistrationDocumentRequest request) {
    Long updatedData = documentService.update(id, request);
    ApiResponse<Long> response =
        ApiResponse.success("REGISTRATION_DOCUMENT_UPDATED_SUCCESSFULLY", updatedData);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/level/{levelId}")
  public ResponseEntity<ApiResponse<List<RegistrationDocumentResponse>>> getDocumentsByLevel(
      @PathVariable Long levelId) {
    List<RegistrationDocumentResponse> data = documentService.getDocumentsByLevelId(levelId);
    ApiResponse<List<RegistrationDocumentResponse>> response =
        ApiResponse.success("DOCUMENTS_RETRIEVED_SUCCESSFULLY", data);
    return ResponseEntity.ok(response);
  }
}
