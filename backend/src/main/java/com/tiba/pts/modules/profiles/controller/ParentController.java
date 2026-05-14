package com.tiba.pts.modules.profiles.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.profiles.dto.request.ExistenceCheckResponse;
import com.tiba.pts.modules.profiles.dto.response.ParentResponse;
import com.tiba.pts.modules.profiles.service.ParentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles/parents")
@RequiredArgsConstructor
public class ParentController {

  private final ParentService parentService;

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SECRETARIAT')")
  public ResponseEntity<ApiResponse<List<ParentResponse>>> searchParents(
      @RequestParam(required = false, name = "keyword") String keyword) {

    List<ParentResponse> parents = parentService.searchParents(keyword);

    ApiResponse<List<ParentResponse>> apiResponse =
        ApiResponse.<List<ParentResponse>>builder()
            .success(true)
            .message("PARENTS_RETRIEVED_SUCCESSFULLY")
            .data(parents)
            .build();

    return ResponseEntity.ok(apiResponse);
  }

  @GetMapping("/check-existence")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ExistenceCheckResponse>> checkParentExistence(
      @RequestParam(required = false) String email, @RequestParam(required = false) String phone) {

    ExistenceCheckResponse existenceData = parentService.checkExistence(email, phone);

    String message = existenceData.hasAnyConflict() ? "CONFLICTS_FOUND" : "NO_CONFLICTS_FOUND";

    ApiResponse<ExistenceCheckResponse> apiResponse =
        ApiResponse.<ExistenceCheckResponse>builder()
            .success(true)
            .message(message)
            .data(existenceData)
            .build();

    return ResponseEntity.ok(apiResponse);
  }
}
