package com.tiba.pts.modules.profiles.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.profiles.domain.entity.RefTeacherSpecialty;
import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import com.tiba.pts.modules.profiles.dto.request.TeacherFiltreRequest;
import com.tiba.pts.modules.profiles.dto.request.TeacherRequest;
import com.tiba.pts.modules.profiles.dto.response.TeacherResponse;
import com.tiba.pts.modules.profiles.dto.response.TeacherSimpleResponse;
import com.tiba.pts.modules.profiles.mapper.TeacherMapper;
import com.tiba.pts.modules.profiles.repository.PersonRepository;
import java.util.List;
import com.tiba.pts.modules.profiles.repository.RefTeacherSpecialtyRepository;
import com.tiba.pts.modules.profiles.repository.TeacherRepository;
import com.tiba.pts.modules.user.domain.enums.Role;
import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeacherService {

  private final TeacherRepository teacherRepository;
  private final RefTeacherSpecialtyRepository specialtyRepository;
  private final TeacherMapper teacherMapper;
  private final UserService userService;
  private final PersonRepository personRepository;

  @Transactional
  public Long createTeacher(TeacherRequest request) {
    // Duplication check (Email, CIN and phone number)
    if (request.getEmail() != null
        && !request.getEmail().isBlank()
        && personRepository.existsByEmail(request.getEmail())) {
      throw new EntityAlreadyExistsException("EMAIL_ALREADY_EXISTS");
    }
    if (request.getCin() != null
        && !request.getCin().isBlank()
        && personRepository.existsByCin(request.getCin())) {
      throw new EntityAlreadyExistsException("CIN_ALREADY_EXISTS");
    }
    if (request.getCin() != null
        && !request.getPhone().isBlank()
        && personRepository.existsByPhone(request.getPhone())) {
      throw new EntityAlreadyExistsException("PHONE_NUMBER_ALREADY_EXISTS");
    }

    // Request to Entity mapping
    Teacher teacher = teacherMapper.toEntity(request);
    // AUTOMATIC CODE GENERATION AND DEFAULT STATUS ASSIGNMENT
    teacher.setCode(generateTeacherCode());

    teacher.setStatus(TeacherStatus.ONBOARDING);

    // Validation and association of specialties
    if (request.getSpecialtyIds() != null && !request.getSpecialtyIds().isEmpty()) {
      Set<RefTeacherSpecialty> specialties =
          specialtyRepository.findByIdIn(request.getSpecialtyIds());
      if (specialties.size() != request.getSpecialtyIds().size()) {
        throw new ResourceNotFoundException("ONE_OR_MORE_SPECIALTIES_NOT_FOUND");
      }
      teacher.setSpecialties(specialties);
    }

    // Database save
    Teacher savedTeacher = teacherRepository.save(teacher);
    return savedTeacher.getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<TeacherResponse> getAllPaged(
      TeacherFiltreRequest filtre, int page, int size) {

    // Default pagination and sorting definition
    Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());

    // Direct Repository call with parameters extracted from the filter
    Page<Teacher> teacherPage =
        teacherRepository.searchTeachers(
            filtre.getKeyword(),
            filtre.getSpecialtyId(),
            filtre.getContractType(),
            filtre.getStatus(),
            pageable);

    // Transformation to standard format
    return PageResponse.of(teacherPage, teacherMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public long countActiveTeachers() {
    return teacherRepository.countByStatus(TeacherStatus.ACTIVE);
  }

  @Transactional(readOnly = true)
  public List<TeacherSimpleResponse> getActiveTeachers() {
    List<Teacher> activeTeachers = teacherRepository.findByStatus(TeacherStatus.ACTIVE);
    return teacherMapper.toSimpleResponseList(activeTeachers);
  }

  @Transactional(readOnly = true)
  public TeacherResponse getTeacherById(Long id) {
    Teacher teacher =
        teacherRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TEACHER_NOT_FOUND"));

    return teacherMapper.toResponse(teacher);
  }

  @Transactional
  public Long updateTeacherStatus(Long id, TeacherStatus status) {
    // Retrieve the teacher
    Teacher teacher =
        teacherRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TEACHER_NOT_FOUND"));

    TeacherStatus oldStatus = teacher.getStatus();
    TeacherStatus newStatus = status;

    // If the status doesn't change, we do nothing to avoid unnecessary calls
    if (oldStatus == newStatus) {
      return teacher.getId();
    }

    // Update the profile status
    teacher.setStatus(newStatus);
    Teacher savedTeacher = teacherRepository.save(teacher);

    // BUSINESS LOGIC: USER ACCOUNT LIFECYCLE
    switch (newStatus) {
      case ACTIVE:
        if (teacher.getUser() == null) {
          // First transition to ACTIVE -> Account creation
          if (teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            throw new RuntimeException("EMAIL_REQUIRED_FOR_ACTIVATION");
          }

          UserCreateRequest userRequest = new UserCreateRequest();
          userRequest.setUsername(teacher.getEmail());
          userRequest.setPassword("1234568"); // Default password
          userRequest.setRole(Role.ROLE_TEACHER);
          userRequest.setPersonId(teacher.getId());

          userService.createUser(userRequest);
        } else {
          // The trainer already had an account (e.g., return from suspension) -> Reactivation
          userService.reactivateUser(teacher.getUser().getId());
        }
        break;

      case SUSPENDED:
        // If suspended, block their system access
        if (teacher.getUser() != null) {
          userService.suspendUser(teacher.getUser().getId());
        }
        break;

      case DEPARTED:
        // If they leave the school, permanently archive their account
        if (teacher.getUser() != null) {
          userService.archiveUser(teacher.getUser().getId());
        }
        break;

      // For ONBOARDING or ON_LEAVE, we can decide to do nothing on the account,
      // or add logic later if necessary.
      default:
        break;
    }

    return savedTeacher.getId();
  }

  @Transactional
  public Long updateTeacher(Long id, TeacherRequest request) {
    // Retrieve the existing teacher
    Teacher teacher =
        teacherRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TEACHER_NOT_FOUND"));

    // Duplication check (Email, CIN and Phone) EXCLUDING the current ID
    if (request.getEmail() != null
        && !request.getEmail().isBlank()
        && personRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
      throw new EntityAlreadyExistsException("EMAIL_ALREADY_EXISTS");
    }

    if (request.getCin() != null
        && !request.getCin().isBlank()
        && personRepository.existsByCinAndIdNot(request.getCin(), id)) {
      throw new EntityAlreadyExistsException("CIN_ALREADY_EXISTS");
    }

    if (request.getPhone() != null
        && !request.getPhone().isBlank()
        && personRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
      throw new EntityAlreadyExistsException("PHONE_NUMBER_ALREADY_EXISTS");
    }

    // Update basic fields via MapStruct
    teacherMapper.updateEntityFromRequest(request, teacher);

    // Update specialties
    if (request.getSpecialtyIds() != null && !request.getSpecialtyIds().isEmpty()) {
      Set<RefTeacherSpecialty> specialties =
          specialtyRepository.findByIdIn(request.getSpecialtyIds());
      if (specialties.size() != request.getSpecialtyIds().size()) {
        throw new ResourceNotFoundException("ONE_OR_MORE_SPECIALTIES_NOT_FOUND");
      }
      teacher.setSpecialties(specialties);
    } else {
      teacher.getSpecialties().clear();
    }

    // Save
    Teacher updatedTeacher = teacherRepository.save(teacher);
    return updatedTeacher.getId();
  }

  /**
   * Private business method to generate the trainer's code. Example return: PROF2026001,
   * PROF2026002...
   */
  private String generateTeacherCode() {
    String year = String.valueOf(LocalDate.now().getYear());
    String prefix = "PROF" + year;

    // Retrieve the highest code starting with PROF2026 (e.g.: PROF2026015)
    String maxCode = teacherRepository.findMaxCodeByPrefix(prefix);

    int sequence = 1;
    if (maxCode != null && maxCode.length() > prefix.length()) {
      try {
        // Extract the numerical part (e.g.: "015" -> 15) and add 1
        String sequenceStr = maxCode.substring(prefix.length());
        sequence = Integer.parseInt(sequenceStr) + 1;
      } catch (NumberFormatException e) {
        // If there's a parsing issue (corrupted data in DB), the sequence remains at 1
        sequence = 1;
      }
    }

    // Format the sequence to 3 digits with leading zeros (e.g.: 1 -> 001, 12 -> 012)
    return prefix + String.format("%03d", sequence);
  }
}
