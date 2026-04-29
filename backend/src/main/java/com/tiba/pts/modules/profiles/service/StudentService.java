package com.tiba.pts.modules.profiles.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.domain.enums.ParentalLink;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.response.StudentResponse;
import com.tiba.pts.modules.profiles.mapper.StudentMapper;
import com.tiba.pts.modules.profiles.repository.PersonRepository;
import com.tiba.pts.modules.profiles.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

  private final StudentRepository studentRepository;
  private final StudentMapper studentMapper;
  private final PersonRepository personRepository;

  @Transactional
  public Long createStudent(StudentRequest request) {

    // Call to the validation function
    validateStudent(request);

    // Mapping
    Student student = studentMapper.toEntity(request);

    // --- brother and sister ---
    if (student.getStudentSiblings() != null) {
      student
          .getStudentSiblings()
          .forEach(
              sibling -> {
                sibling.setStudent(student);
              });
    }

    // Automatic generation of the student code
    String generatedCode = generateStudentCode();
    student.setStudentCode(generatedCode);

    // Link the Parents (if the list is not empty)
    if (student.getParents() != null) {
      student.getParents().forEach(parentRelation -> parentRelation.setStudent(student));
    }

    return studentRepository.save(student).getId();
  }

  /** Private method to generate the student code in YYMM*** format */
  private String generateStudentCode() {
    // Retrieve the current year and month (ex: 2026-04)
    YearMonth currentYearMonth = YearMonth.now();

    // Format as YYMM (ex: "2604")
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMM");
    String prefix = currentYearMonth.format(formatter);

    // Search for the last existing code in the database that starts with "2604"
    Optional<Student> lastStudent =
        studentRepository.findTopByStudentCodeStartingWithOrderByStudentCodeDesc(prefix);

    int sequence = 1; // By default, we start at 1

    if (lastStudent.isPresent()) {
      String lastCode = lastStudent.get().getStudentCode();

      // We extract the numeric part (everything after the first 4 characters "2604")
      String sequenceStr = lastCode.substring(prefix.length());
      sequence = Integer.parseInt(sequenceStr) + 1; // We increment by 1
    }

    // We assemble the prefix and the sequence formatted to 3 digits (ex: 001, 002, 010...)
    return prefix + String.format("%03d", sequence);
  }

  private void validateStudent(StudentRequest request) {
    List<ErrorDetail> errors = new ArrayList<>();

    // ==========================================
    // STUDENT VALIDATION
    // ==========================================
    if (request.getCin() != null && !request.getCin().isEmpty()) {
      if (personRepository.existsByCin(request.getCin())) {
        errors.add(new ErrorDetail("student.cin", "CIN_STUDENT_ALREADY_EXISTS"));
      }
    }

    if (request.getEmail() != null && !request.getEmail().isEmpty()) {
      if (personRepository.existsByEmail(request.getEmail())) {
        errors.add(new ErrorDetail("student.email", "EMAIL_STUDENT_ALREADY_EXISTS"));
      }
    }

    if (request.getPhone() != null && !request.getPhone().isEmpty()) {
      if (personRepository.existsByPhone(request.getPhone())) {
        errors.add(new ErrorDetail("student.phone", "PHONE_STUDENT_ALREADY_EXISTS"));
      }
    }

    // ==========================================
    // PARENTS VALIDATION (Loop)
    // ==========================================
    if (request.getParents() != null) {
      for (int i = 0; i < request.getParents().size(); i++) {

        var parentInfo = request.getParents().get(i).getParent();
        ParentalLink prefix = request.getParents().get(i).getLink();

        // Parent Email Verification
        if (parentInfo.getEmail() != null && !parentInfo.getEmail().isEmpty()) {
          if (personRepository.existsByEmail(parentInfo.getEmail())) {
            errors.add(new ErrorDetail(prefix + "_EMAIL", "EMAIL_ALREADY_EXISTS"));
          }
        }

        // Parent Phone Verification
        if (parentInfo.getPhone() != null && !parentInfo.getPhone().isEmpty()) {
          if (personRepository.existsByPhone(parentInfo.getPhone())) {
            errors.add(new ErrorDetail(prefix + "_PHONE", "PHONE_ALREADY_EXISTS"));
          }
        }

        // Parent CIN Verification
        if (parentInfo.getCin() != null && !parentInfo.getCin().isEmpty()) {
          if (personRepository.existsByCin(parentInfo.getCin())) {
            errors.add(new ErrorDetail(prefix + "_CIN", "CIN_ALREADY_EXISTS"));
          }
        }
      }
    }

    // ==========================================
    // EXCEPTION TRIGGERING
    // ==========================================
    if (!errors.isEmpty()) {
      throw new BusinessValidationException("STUDENT_CREATION_VALIDATION_FAILED", errors);
    }
  }

  @Transactional(readOnly = true)
  public StudentResponse getStudentById(Long id) {
    Student student =
        studentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND"));

    return studentMapper.toResponse(student);
  }
}
