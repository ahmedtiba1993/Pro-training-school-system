package com.tiba.pts.modules.user.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.modules.profiles.domain.entity.Person;
import com.tiba.pts.modules.profiles.repository.PersonRepository;
import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.entity.User;
import com.tiba.pts.modules.user.mapper.UserMapper;
import com.tiba.pts.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
}
