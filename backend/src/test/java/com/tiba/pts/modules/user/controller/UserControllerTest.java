package com.tiba.pts.modules.user.controller;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.GlobalExceptionHandler;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import com.tiba.pts.modules.user.dto.request.ChangePasswordRequest;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import com.tiba.pts.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  private MockMvc mockMvc;

  @Mock
  private UserService userService;

  @InjectMocks
  private UserController userController;

  private final User testUser =
      User.builder().id(1L).username("testuser").role(Role.ROLE_ADMIN).status(UserStatus.ACTIVE).build();

  @BeforeEach
  void setUp() {
    HandlerMethodArgumentResolver authPrincipalResolver =
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return testUser;
          }
        };

    mockMvc =
        MockMvcBuilders.standaloneSetup(userController)
            .setCustomArgumentResolvers(authPrincipalResolver)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void getUserById_whenUserExists_shouldReturn200AndUserResponse() throws Exception {
    Long userId = 1L;
    UserResponse response =
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

    when(userService.getUserById(userId)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/{id}", userId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("USER_RETRIEVED"))
        .andExpect(jsonPath("$.data.id").value(userId))
        .andExpect(jsonPath("$.data.username").value("teacher_john"))
        .andExpect(jsonPath("$.data.role").value("ROLE_TEACHER"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.firstName").value("John"))
        .andExpect(jsonPath("$.data.lastName").value("Doe"))
        .andExpect(jsonPath("$.data.phone").value("+123456789"))
        .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));
  }

  @Test
  void getUserById_whenUserNotFound_shouldReturn404() throws Exception {
    Long userId = 999L;
    when(userService.getUserById(userId))
        .thenThrow(new ResourceNotFoundException("USER_NOT_FOUND"));

    mockMvc
        .perform(get("/api/v1/users/{id}", userId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("USER_NOT_FOUND"));
  }

  @Test
  void getUserById_whenAccessDenied_shouldReturn403() throws Exception {
    Long userId = 2L;
    when(userService.getUserById(userId))
        .thenThrow(new AccessDeniedException("ERR_ACCESS_DENIED"));

    mockMvc
        .perform(get("/api/v1/users/{id}", userId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_ACCESS_DENIED"));
  }

  @Test
  void getUserByUsername_whenUserExists_shouldReturn200AndUserResponse() throws Exception {
    String username = "teacher_john";
    UserResponse response =
        UserResponse.builder()
            .id(1L)
            .username(username)
            .role(Role.ROLE_TEACHER)
            .status(UserStatus.ACTIVE)
            .forcePasswordChange(false)
            .personId(10L)
            .firstName("John")
            .lastName("Doe")
            .phone("+123456789")
            .email("john.doe@example.com")
            .build();

    when(userService.getUserByUsername(username)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/users/username/{username}", username).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("USER_RETRIEVED"))
        .andExpect(jsonPath("$.data.id").value(1L))
        .andExpect(jsonPath("$.data.username").value(username))
        .andExpect(jsonPath("$.data.role").value("ROLE_TEACHER"))
        .andExpect(jsonPath("$.data.firstName").value("John"))
        .andExpect(jsonPath("$.data.lastName").value("Doe"));
  }

  @Test
  void getUserByUsername_whenUserNotFound_shouldReturn404() throws Exception {
    String username = "unknown_user";
    when(userService.getUserByUsername(username))
        .thenThrow(new ResourceNotFoundException("USER_NOT_FOUND"));

    mockMvc
        .perform(get("/api/v1/users/username/{username}", username).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("USER_NOT_FOUND"));
  }

  @Test
  void getUserByUsername_whenAccessDenied_shouldReturn403() throws Exception {
    String username = "other_user";
    when(userService.getUserByUsername(username))
        .thenThrow(new AccessDeniedException("ERR_ACCESS_DENIED"));

    mockMvc
        .perform(get("/api/v1/users/username/{username}", username).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_ACCESS_DENIED"));
  }

  @Test
  void activateUser_whenSuccess_shouldReturn200() throws Exception {
    Long userId = 1L;
    doNothing().when(userService).activateUser(userId);

    mockMvc
        .perform(post("/api/v1/users/{id}/activate", userId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("USER_ACTIVATED_SUCCESSFULLY"));

    verify(userService).activateUser(userId);
  }

  @Test
  void activateUser_whenStatusInvalid_shouldReturn409() throws Exception {
    Long userId = 1L;
    doThrow(new BusinessValidationException("USER_INVALID_STATUS_FOR_ACTIVATION"))
        .when(userService)
        .activateUser(userId);

    mockMvc
        .perform(post("/api/v1/users/{id}/activate", userId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
        .andExpect(jsonPath("$.message").value("USER_INVALID_STATUS_FOR_ACTIVATION"));

    verify(userService).activateUser(userId);
  }

  @Test
  void changePassword_whenValidRequest_shouldReturn200() throws Exception {
    doNothing().when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

    String requestBody =
        """
        {
          "oldPassword": "oldPassword123",
          "newPassword": "newPassword123",
          "confirmNewPassword": "newPassword123"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("PASSWORD_CHANGED_SUCCESSFULLY"));

    verify(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));
  }

  @Test
  void changePassword_whenPasswordTooShort_shouldReturn400() throws Exception {
    String requestBody =
        """
        {
          "oldPassword": "oldPassword123",
          "newPassword": "short",
          "confirmNewPassword": "short"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));

    verifyNoInteractions(userService);
  }

  @Test
  void changePassword_whenPasswordsDoNotMatch_shouldReturn400() throws Exception {
    String requestBody =
        """
        {
          "oldPassword": "oldPassword123",
          "newPassword": "newPassword123",
          "confirmNewPassword": "differentPassword123"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/users/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));

    verifyNoInteractions(userService);
  }
}
