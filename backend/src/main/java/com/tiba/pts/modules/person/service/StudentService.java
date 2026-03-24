package com.tiba.pts.modules.person.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.modules.person.domain.entity.Student;
import com.tiba.pts.modules.person.dto.request.StudentRequest;
import com.tiba.pts.modules.person.mapper.StudentMapper;
import com.tiba.pts.modules.person.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

  private final StudentRepository studentRepository;
  private final StudentMapper studentMapper;

  @Transactional
  public Student createStudent(StudentRequest request) {
    validateStudentUniqueness(request);
    Student student = studentMapper.toEntity(request);
    return studentRepository.save(student);
  }

  private void validateStudentUniqueness(StudentRequest request) {

    List<ErrorDetail> conflicts = new ArrayList<>();

    // Check Email
    if (StringUtils.hasText(request.getEmail())
        && studentRepository.existsByEmail(request.getEmail())) {
      conflicts.add(new ErrorDetail("email", "EMAIL_ALREADY_EXISTS"));
    }

    // Check Phone Number
    if (StringUtils.hasText(request.getPhoneNumber())
        && studentRepository.existsByPhoneNumber(request.getPhoneNumber())) {
      conflicts.add(new ErrorDetail("phoneNumber", "PHONE_NUMBER_ALREADY_EXISTS"));
    }

    // Check CIN (Only if provided)
    if (StringUtils.hasText(request.getCin()) && studentRepository.existsByCin(request.getCin())) {
      conflicts.add(new ErrorDetail("cin", "CIN_ALREADY_EXISTS"));
    }

    if (!conflicts.isEmpty()) {
      throw new BusinessValidationException("RESOURCE_DUPLICATION_ERROR", conflicts);
    }
  }
}
