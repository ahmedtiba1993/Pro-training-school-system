package com.tiba.pts.modules.user.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.dto.request.AdminChangePasswordRequest;
import com.tiba.pts.modules.user.dto.request.ChangePasswordRequest;
import com.tiba.pts.modules.user.dto.request.FirstLoginPasswordChangeRequest;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.tiba.pts.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/users"})
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
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

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
    UserResponse user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success("USER_RETRIEVED", user));
  }

  @GetMapping("/username/{username}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(
      @PathVariable String username) {
    UserResponse user = userService.getUserByUsername(username);
    return ResponseEntity.ok(ApiResponse.success("USER_RETRIEVED", user));
  }

  @PatchMapping("/{id}/password")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @PathVariable Long id, @Valid @RequestBody AdminChangePasswordRequest request) {

    userService.resetPassword(id, request);
    ApiResponse<Void> response = ApiResponse.success("PASSWORD_CHANGED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/change-password")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody ChangePasswordRequest request) {

    userService.changePassword(currentUser.getId(), request);
    return ResponseEntity.ok(ApiResponse.success("PASSWORD_CHANGED_SUCCESSFULLY", null));
  }

  @PostMapping("/change-password-first-login")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> changePasswordFirstLogin(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody FirstLoginPasswordChangeRequest request) {

    userService.changePasswordFirstLogin(currentUser.getId(), request);
    ApiResponse<Void> response = ApiResponse.success("PASSWORD_CHANGED_SUCCESSFULLY", null);
    return ResponseEntity.ok(response);
  }

  // Suspend access
  @PostMapping("/{id}/suspend")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable Long id) {

    userService.suspendUser(id);
    return ResponseEntity.ok(ApiResponse.success("USER_SUSPENDED_SUCCESSFULLY", null));
  }

  // Activate access
  @PostMapping("/{id}/activate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long id) {

    userService.activateUser(id);

    return ResponseEntity.ok(ApiResponse.success("USER_ACTIVATED_SUCCESSFULLY", null));
  }

  // Archive account
  @PostMapping("/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> archiveUser(@PathVariable Long id) {

    userService.archiveUser(id);

    return ResponseEntity.ok(ApiResponse.success("USER_ARCHIVED_SUCCESSFULLY", null));
  }
}
