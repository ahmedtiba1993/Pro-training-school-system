package com.tiba.pts.modules.user.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import com.tiba.pts.modules.user.mapper.UserMapper;
import com.tiba.pts.modules.user.repository.UserRepository;
import com.tiba.pts.modules.auth.service.RefreshTokenService;
import com.tiba.pts.modules.user.dto.request.ChangePasswordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(User user) {
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(user);
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void getUserById_whenSelfUser_shouldReturnUserResponseWithPersonDetails() {
    Long userId = 1L;
    User user =
        User.builder()
            .id(userId)
            .username("teacher_john")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(false)
            .build();

    setAuthenticatedUser(user);

    UserResponse expectedResponse =
        UserResponse.builder()
            .id(userId)
            .username("teacher_john")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(false)
            .personId(10L)
            .firstName("John")
            .lastName("Doe")
            .phone("+123456789")
            .email("john.doe@example.com")
            .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.getUserById(userId);

    assertNotNull(actualResponse);
    assertEquals(userId, actualResponse.getId());
    assertEquals("teacher_john", actualResponse.getUsername());
    assertEquals("John", actualResponse.getFirstName());
    assertEquals("Doe", actualResponse.getLastName());
    assertEquals("+123456789", actualResponse.getPhone());
    assertEquals("john.doe@example.com", actualResponse.getEmail());

    verify(userRepository).findById(userId);
    verify(userMapper).toResponse(user);
  }

  @Test
  void getUserById_whenAdmin_shouldReturnAnyUserResponse() {
    Long targetUserId = 2L;
    User admin =
        User.builder()
            .id(99L)
            .username("admin")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

    User targetUser =
        User.builder()
            .id(targetUserId)
            .username("student_bob")
            .role(Role.ROLE_STUDENT)
            .status(UserStatus.ACTIVE)
            .build();

    UserResponse expectedResponse =
        UserResponse.builder().id(targetUserId).username("student_bob").role(Role.ROLE_STUDENT).build();

    setAuthenticatedUser(admin);
    when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
    when(userMapper.toResponse(targetUser)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.getUserById(targetUserId);

    assertNotNull(actualResponse);
    assertEquals(targetUserId, actualResponse.getId());
    verify(userRepository).findById(targetUserId);
  }

  @Test
  void getUserById_whenOtherUserAndNotAdmin_shouldThrowAccessDeniedException() {
    Long targetUserId = 2L;
    User currentUser =
        User.builder()
            .id(1L)
            .username("teacher_john")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .build();

    setAuthenticatedUser(currentUser);

    AccessDeniedException exception =
        assertThrows(AccessDeniedException.class, () -> userService.getUserById(targetUserId));

    assertEquals("ERR_ACCESS_DENIED", exception.getMessage());
    verifyNoInteractions(userRepository);
    verifyNoInteractions(userMapper);
  }

  @Test
  void getUserById_whenNotAuthenticated_shouldThrowAccessDeniedException() {
    SecurityContextHolder.clearContext();

    AccessDeniedException exception =
        assertThrows(AccessDeniedException.class, () -> userService.getUserById(1L));

    assertEquals("ERR_ACCESS_DENIED", exception.getMessage());
    verifyNoInteractions(userRepository);
  }

  @Test
  void getUserById_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
    Long userId = 999L;
    User admin =
        User.builder()
            .id(1L)
            .username("admin")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();
    setAuthenticatedUser(admin);

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));

    assertEquals("USER_NOT_FOUND", exception.getMessage());
    verify(userRepository).findById(userId);
    verifyNoInteractions(userMapper);
  }

  @Test
  void getUserByUsername_whenSelfUser_shouldReturnUserResponse() {
    String username = "teacher_john";
    User user =
        User.builder()
            .id(1L)
            .username(username)
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .build();

    setAuthenticatedUser(user);

    UserResponse expectedResponse =
        UserResponse.builder().id(1L).username(username).role(Role.ROLE_TEACHER).build();

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.getUserByUsername(username);

    assertNotNull(actualResponse);
    assertEquals(username, actualResponse.getUsername());
    verify(userRepository).findByUsername(username);
    verify(userMapper).toResponse(user);
  }

  @Test
  void getUserByUsername_whenAdmin_shouldReturnAnyUserResponse() {
    String targetUsername = "teacher_john";
    User admin =
        User.builder()
            .id(99L)
            .username("admin")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

    User targetUser =
        User.builder()
            .id(1L)
            .username(targetUsername)
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .build();

    UserResponse expectedResponse =
        UserResponse.builder().id(1L).username(targetUsername).role(Role.ROLE_TEACHER).build();

    setAuthenticatedUser(admin);
    when(userRepository.findByUsername(targetUsername)).thenReturn(Optional.of(targetUser));
    when(userMapper.toResponse(targetUser)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.getUserByUsername(targetUsername);

    assertNotNull(actualResponse);
    assertEquals(targetUsername, actualResponse.getUsername());
    verify(userRepository).findByUsername(targetUsername);
  }

  @Test
  void getUserByUsername_whenOtherUserAndNotAdmin_shouldThrowAccessDeniedException() {
    User currentUser =
        User.builder()
            .id(1L)
            .username("teacher_john")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .build();

    setAuthenticatedUser(currentUser);

    AccessDeniedException exception =
        assertThrows(AccessDeniedException.class, () -> userService.getUserByUsername("other_user"));

    assertEquals("ERR_ACCESS_DENIED", exception.getMessage());
    verifyNoInteractions(userRepository);
    verifyNoInteractions(userMapper);
  }

  @Test
  void getUserByUsername_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
    String username = "unknown_user";
    User admin =
        User.builder()
            .id(1L)
            .username("admin")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();
    setAuthenticatedUser(admin);

    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByUsername(username));

    assertEquals("USER_NOT_FOUND", exception.getMessage());
    verify(userRepository).findByUsername(username);
    verifyNoInteractions(userMapper);
  }

  @Test
  void activateUser_whenUserPending_shouldSetActiveAndSave() {
    Long userId = 1L;
    User user =
        User.builder()
            .id(userId)
            .username("pending_user")
            .role(Role.ROLE_STUDENT)
            .status(UserStatus.PENDING)
            .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.activateUser(userId);

    assertEquals(UserStatus.ACTIVE, user.getStatus());
    verify(userRepository).save(user);
  }

  @Test
  void activateUser_whenUserSuspended_shouldSetActiveAndSave() {
    Long userId = 2L;
    User user =
        User.builder()
            .id(userId)
            .username("suspended_user")
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.SUSPENDED)
            .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.activateUser(userId);

    assertEquals(UserStatus.ACTIVE, user.getStatus());
    verify(userRepository).save(user);
  }

  @Test
  void activateUser_whenUserAlreadyActive_shouldThrowBusinessValidationException() {
    Long userId = 3L;
    User user =
        User.builder()
            .id(userId)
            .username("active_user")
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    BusinessValidationException exception =
        assertThrows(BusinessValidationException.class, () -> userService.activateUser(userId));

    assertEquals("USER_INVALID_STATUS_FOR_ACTIVATION", exception.getMessage());
    verify(userRepository, never()).save(any());
  }

  @Test
  void activateUser_whenUserArchived_shouldThrowBusinessValidationException() {
    Long userId = 4L;
    User user =
        User.builder()
            .id(userId)
            .username("archived_user")
            .role(Role.ROLE_STUDENT)
            .status(UserStatus.ARCHIVED)
            .build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    BusinessValidationException exception =
        assertThrows(BusinessValidationException.class, () -> userService.activateUser(userId));

    assertEquals("USER_INVALID_STATUS_FOR_ACTIVATION", exception.getMessage());
    verify(userRepository, never()).save(any());
  }

  @Test
  void activateUser_whenUserNotFound_shouldThrowResourceNotFoundException() {
    Long userId = 999L;
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.activateUser(userId));

    assertEquals("USER_NOT_FOUND", exception.getMessage());
    verify(userRepository, never()).save(any());
  }

  @Test
  void changePassword_whenValid_shouldUpdatePasswordAndRevokeTokens() {
    Long userId = 1L;
    User user =
        User.builder()
            .id(userId)
            .username("student_alex")
            .password("encodedOldPassword")
            .status(UserStatus.PENDING)
            .forcePasswordChange(true)
            .build();

    ChangePasswordRequest request =
        new ChangePasswordRequest("oldSecret123", "newSecret123", "newSecret123");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("oldSecret123", "encodedOldPassword")).thenReturn(true);
    when(passwordEncoder.matches("newSecret123", "encodedOldPassword")).thenReturn(false);
    when(passwordEncoder.encode("newSecret123")).thenReturn("encodedNewPassword");

    userService.changePassword(userId, request);

    assertEquals("encodedNewPassword", user.getPassword());
    assertFalse(user.isForcePasswordChange());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    verify(userRepository).save(user);
    verify(refreshTokenService).revokeAllUserTokens(user);
  }

  @Test
  void changePassword_whenOldPasswordIncorrect_shouldThrowBusinessValidationException() {
    Long userId = 1L;
    User user =
        User.builder()
            .id(userId)
            .username("student_alex")
            .password("encodedOldPassword")
            .build();

    ChangePasswordRequest request =
        new ChangePasswordRequest("wrongPassword", "newSecret123", "newSecret123");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

    BusinessValidationException exception =
        assertThrows(BusinessValidationException.class, () -> userService.changePassword(userId, request));

    assertEquals("USER_OLD_PASSWORD_INCORRECT", exception.getMessage());
    verify(userRepository, never()).save(any());
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void changePassword_whenPasswordReused_shouldThrowBusinessValidationException() {
    Long userId = 1L;
    User user =
        User.builder()
            .id(userId)
            .username("student_alex")
            .password("encodedOldPassword")
            .build();

    ChangePasswordRequest request =
        new ChangePasswordRequest("oldSecret123", "oldSecret123", "oldSecret123");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("oldSecret123", "encodedOldPassword")).thenReturn(true);

    BusinessValidationException exception =
        assertThrows(BusinessValidationException.class, () -> userService.changePassword(userId, request));

    assertEquals("USER_PASSWORD_CANNOT_BE_REUSED", exception.getMessage());
    verify(userRepository, never()).save(any());
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void changePassword_whenUserNotFound_shouldThrowResourceNotFoundException() {
    Long userId = 999L;
    ChangePasswordRequest request =
        new ChangePasswordRequest("oldSecret123", "newSecret123", "newSecret123");

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> userService.changePassword(userId, request));

    assertEquals("USER_NOT_FOUND", exception.getMessage());
    verify(userRepository, never()).save(any());
    verifyNoInteractions(refreshTokenService);
  }
}
