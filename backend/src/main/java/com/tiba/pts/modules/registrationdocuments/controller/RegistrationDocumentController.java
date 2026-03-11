package com.tiba.pts.modules.registrationdocuments.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.registrationdocuments.dto.RegistrationDocumentRequest;
import com.tiba.pts.modules.registrationdocuments.service.RegistrationDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
