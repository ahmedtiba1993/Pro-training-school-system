package com.tiba.pts.modules.user.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.modules.profiles.domain.entity.Person;
import com.tiba.pts.modules.profiles.repository.PersonRepository;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.domain.enums.UserStatus;
import com.tiba.pts.modules.user.dto.request.AdminChangePasswordRequest;
import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import com.tiba.pts.modules.user.mapper.UserMapper;
import com.tiba.pts.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final PersonRepository personRepository;

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
  public void changeUserPassword(Long userId, AdminChangePasswordRequest request) {

    // Existence check
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

    // Hashing and update
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));

    // Security: Force change at next login
    user.setForcePasswordChange(true);

    userRepository.save(user);
  }

  /** Temporarily suspends a user's access */
  @Transactional
  public void suspendUser(Long userId) {
    updateUserStatus(userId, UserStatus.SUSPENDED);
  }

  /** Reactivates a user (previously suspended or pending) */
  @Transactional
  public void reactivateUser(Long userId) {
    updateUserStatus(userId, UserStatus.ACTIVE);
  }

  /** Permanently archives the account (Soft Delete) */
  @Transactional
  public void archiveUser(Long userId) {
    updateUserStatus(userId, UserStatus.ARCHIVED);
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
}
