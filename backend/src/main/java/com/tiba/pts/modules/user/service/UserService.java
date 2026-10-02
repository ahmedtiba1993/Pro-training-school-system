package com.tiba.pts.modules.user.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.modules.auth.service.RefreshTokenService;
import com.tiba.pts.modules.profiles.domain.entity.Person;
import com.tiba.pts.modules.profiles.repository.PersonRepository;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.user.dto.request.AdminChangePasswordRequest;
import com.tiba.pts.modules.user.dto.request.ChangePasswordRequest;
import com.tiba.pts.modules.user.dto.request.FirstLoginPasswordChangeRequest;
import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import com.tiba.pts.modules.user.mapper.UserMapper;
import com.tiba.pts.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final PersonRepository personRepository;
  private final RefreshTokenService refreshTokenService;

  @Transactional
  public void createUser(UserCreateRequest request) {
    // Check existence
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new EntityAlreadyExistsException("USERNAME_ALREADY_EXISTS");
    }

    // Mapping
    User user = userMapper.toEntity(request);

    // Security: Password encoding
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    // Assign default role
    user.setRole(request.getRole());
    user.setStatus(UserStatus.PENDING);
    // Link the Person (Student, Parent, etc.) if an ID is provided
    if (request.getPersonId() != null) {
      Person person =
          personRepository
              .findById(request.getPersonId())
              .orElseThrow(() -> new EntityNotFoundException("PERSON_NOT_FOUND"));
      user.setPerson(person);
    }

    // Save
    userRepository.save(user);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Transactional(readOnly = true)
  public Optional<User> findByPersonId(Long personId) {
    return userRepository.findByPersonId(personId);
  }

  @Transactional(readOnly = true)
  public UserResponse getUserById(Long id) {
    validateUserOwnership(id, null);
    return userRepository
        .findById(id)
        .map(userMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public UserResponse getUserByUsername(String username) {
    validateUserOwnership(null, username);
    return userRepository
        .findByUsername(username)
        .map(userMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));
  }

  /**
   * Retrieves the filtered and paginated list of users. Transactional in readOnly to optimize
   * database access.
   */
  @Transactional(readOnly = true)
  public PageResponse<UserResponse> getAllUsersPaged(
      String keyword, Role role, UserStatus status, int page, int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

    Page<User> pageResult = userRepository.findAllFiltered(keyword, role, status, pageable);

    return PageResponse.of(pageResult, userMapper::toResponse);
  }

  @Transactional
  public void resetPassword(Long userId, AdminChangePasswordRequest request) {

    // Existence check
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

    String adminUsername = "SYSTEM";
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      adminUsername = auth.getName();
    }

    // Hashing and update
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));

    // Security: Force change at next login
    user.setForcePasswordChange(true);

    userRepository.save(user);

    // 5. CRITICAL: Immediately disconnect the user from all devices
    // If their account was compromised, this instantly cuts off access to the attacker.
    refreshTokenService.revokeAllUserTokens(user);

    // 6. Audit: Trace the action in the server logs
    log.info(
        "SECURITY: User password with ID {} has been reset by administrator {}",
        userId,
        adminUsername);
  }

  /**
   * Changes the password for the given user after validating the old password,
   * complexity, and preventing reuse. Revokes all active refresh tokens.
   *
   * @param userId the ID of the user changing their password
   * @param request the ChangePasswordRequest containing old and new passwords
   */
  @Transactional
  public void changePassword(Long userId, ChangePasswordRequest request) {
    // 1. Lookup user
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

    // 2. Verify old password
    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
      throw new BusinessValidationException("USER_OLD_PASSWORD_INCORRECT");
    }

    // 3. Forbid reusing the same password
    if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
      throw new BusinessValidationException("USER_PASSWORD_CANNOT_BE_REUSED");
    }

    // 4. Update password and workflow flags
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    user.setForcePasswordChange(false);
    if (user.getStatus() == UserStatus.PENDING) {
      user.setStatus(UserStatus.ACTIVE);
    }
    userRepository.save(user);

    // 5. Secure logout: Revoke all other active sessions (Refresh Tokens)
    refreshTokenService.revokeAllUserTokens(user);

    // 6. Audit log
    log.info("SECURITY: Password successfully changed for user ID {}", userId);
  }

  /**
   * Temporarily suspends a user's access with administrative protections and immediate token
   * revocation.
   *
   * @param userId the ID of the user to suspend
   */
  @Transactional
  public void suspendUser(Long userId) {
    // 1. Admin Protection: An administrator cannot suspend themselves
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User currentUser) {
      if (userId.equals(currentUser.getId())) {
        throw new BusinessValidationException("USER_CANNOT_SUSPEND_SELF");
      }
    }

    // 2. Lookup user
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

    // Check if already suspended
    if (user.getStatus() == UserStatus.SUSPENDED) {
      throw new BusinessValidationException("USER_ALREADY_SUSPENDED");
    }

    // 3. System Protection: At least one active Administrator must remain
    if (user.getRole() == Role.ROLE_ADMIN && user.getStatus() == UserStatus.ACTIVE) {
      long activeAdminCount =
          userRepository.countByRoleAndStatus(Role.ROLE_ADMIN, UserStatus.ACTIVE);
      if (activeAdminCount <= 1) {
        throw new BusinessValidationException("USER_LAST_ACTIVE_ADMIN_CANNOT_BE_SUSPENDED");
      }
    }

    // 4. Update status
    user.setStatus(UserStatus.SUSPENDED);
    userRepository.save(user);

    // 5. Immediate effect: Revoke all active refresh tokens
    refreshTokenService.revokeAllUserTokens(user);
  }

  /**
   * Activates a user account. State condition: only applicable if the user is currently PENDING or
   * SUSPENDED.
   *
   * @param userId the ID of the user to activate
   */
  @Transactional
  public void activateUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

    if (user.getStatus() != UserStatus.PENDING && user.getStatus() != UserStatus.SUSPENDED) {
      throw new BusinessValidationException("USER_INVALID_STATUS_FOR_ACTIVATION");
    }

    user.setStatus(UserStatus.ACTIVE);
    userRepository.save(user);
  }

  /**
   * Permanently archives a user account (Soft Delete) and revokes all active tokens.
   *
   * @param userId the ID of the user to archive
   */
  @Transactional
  public void archiveUser(Long userId) {
    // 1. Admin Protection: An administrator cannot archive themselves
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof User currentUser) {
      if (userId.equals(currentUser.getId())) {
        throw new BusinessValidationException("USER_CANNOT_ARCHIVE_SELF");
      }
    }

    // 2. Lookup user
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

    // Check if already archived
    if (user.getStatus() == UserStatus.ARCHIVED) {
      throw new BusinessValidationException("USER_ALREADY_ARCHIVED");
    }

    // 3. System Protection: At least one active Administrator must remain
    if (user.getRole() == Role.ROLE_ADMIN && user.getStatus() == UserStatus.ACTIVE) {
      long activeAdminCount =
          userRepository.countByRoleAndStatus(Role.ROLE_ADMIN, UserStatus.ACTIVE);
      if (activeAdminCount <= 1) {
        throw new BusinessValidationException("USER_LAST_ACTIVE_ADMIN_CANNOT_BE_ARCHIVED");
      }
    }

    // 4. Update status
    user.setStatus(UserStatus.ARCHIVED);
    userRepository.save(user);

    // 5. Immediate effect: Revoke all active refresh tokens
    refreshTokenService.revokeAllUserTokens(user);
  }

  /** Private method (DRY) to centralize the status change logic */
  public void updateUserStatus(Long userId, UserStatus newStatus) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

    user.setStatus(newStatus);
    userRepository.save(user);
  }

  @Transactional
  public void updateUserStatusByPersonIdSafe(Long personId, UserStatus newStatus) {
    Optional<User> userOpt = userRepository.findByPersonId(personId);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      user.setStatus(newStatus);
      userRepository.save(user);
    }
  }

  @Transactional
  public void changePasswordFirstLogin(Long userId, FirstLoginPasswordChangeRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

    if (!user.isForcePasswordChange()) {
      throw new BusinessValidationException("PASSWORD_CHANGE_NOT_FORCED");
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    user.setForcePasswordChange(false);

    if (user.getStatus() == UserStatus.PENDING) {
      user.setStatus(UserStatus.ACTIVE);
    }

    userRepository.save(user);
  }

  /**
   * BOLA / IDOR protection: Non-admin users are strictly restricted to accessing their own
   * profile (either by ID or username). Administrators have full access.
   */
  private void validateUserOwnership(Long targetId, String targetUsername) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof User currentUser)) {
      throw new AccessDeniedException("ERR_ACCESS_DENIED");
    }

    if (currentUser.getRole() == Role.ROLE_ADMIN) {
      return;
    }

    if (targetId != null && !targetId.equals(currentUser.getId())) {
      throw new AccessDeniedException("ERR_ACCESS_DENIED");
    }

    if (targetUsername != null && !targetUsername.equalsIgnoreCase(currentUser.getUsername())) {
      throw new AccessDeniedException("ERR_ACCESS_DENIED");
    }
  }
}
