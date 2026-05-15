package com.tiba.pts.modules.user.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import com.tiba.pts.modules.user.dto.request.AdminChangePasswordRequest;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import com.tiba.pts.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsersPaged(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    PageResponse<UserResponse> pageResult =
        userService.getAllUsersPaged(keyword, role, status, page, size);

    ApiResponse<PageResponse<UserResponse>> response =
        ApiResponse.success("USERS_PAGED_RETRIEVED", pageResult);

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/password")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @PathVariable Long id, @Valid @RequestBody AdminChangePasswordRequest request) {

    userService.changeUserPassword(id, request);
    ApiResponse<Void> response = ApiResponse.success("PASSWORD_CHANGED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  // Suspend access
  @PostMapping("/{id}/suspend")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable Long id) {

    userService.suspendUser(id);
    return ResponseEntity.ok(ApiResponse.success("USER_SUSPENDED_SUCCESSFULLY", null));
  }

  // Reactivate access
  @PostMapping("/{id}/reactivate")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> reactivateUser(@PathVariable Long id) {

    userService.reactivateUser(id);

    return ResponseEntity.ok(ApiResponse.success("USER_REACTIVATED_SUCCESSFULLY", null));
  }

  // Archive account
  @PostMapping("/{id}/archive")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> archiveUser(@PathVariable Long id) {

    userService.archiveUser(id);

    return ResponseEntity.ok(ApiResponse.success("USER_ARCHIVED_SUCCESSFULLY", null));
  }
}
