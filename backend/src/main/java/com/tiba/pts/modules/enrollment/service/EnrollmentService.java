package com.tiba.pts.modules.enrollment.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.documents.repository.EnrollmentDocumentRepository;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentRequest;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentListResponse;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.mapper.EnrollmentMapper;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.repository.StudentRepository;
import com.tiba.pts.modules.profiles.service.StudentService;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

  private final EnrollmentRepository enrollmentRepository;
  private final EnrollmentMapper enrollmentMapper;
  private final PromotionRepository promotionRepository;
  private final EnrollmentDocumentRepository documentRepository;
  private final StudentRepository studentRepository;
  private final StudentService studentService;

  @Transactional
  public EnrollmentResponse create(EnrollmentRequest request) {

    // Promotion validation
    Promotion promotion =
        promotionRepository
            .findById(request.getPromotionId())
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    // Student creation (Student + Siblings + Parents) via the Profiles module
    StudentRequest profileRequest = enrollmentMapper.toStudentRequest(request.getStudentInfo());
    Long studentId = studentService.createStudent(profileRequest);
    Student student = studentRepository.getReferenceById(studentId);

    // Enrollment process continuation
    Enrollment enrollment = enrollmentMapper.toEntity(request);
    enrollment.setStudent(student);
    enrollment.setPromotion(promotion);
    enrollment.setEnrollmentNumber(generateEnrollmentNumber());
    enrollment.setStatus(EnrollmentStatus.PRE_ENROLLED);

// Document synchronization
    if (enrollment.getEnrollmentDocumentSubmissions() != null) {
      enrollment
          .getEnrollmentDocumentSubmissions()
          .forEach(
              doc -> {
                doc.setEnrollment(enrollment);
                EnrollmentDocument realDocument =
                    documentRepository
                        .findById(doc.getDocument().getId())
                        .orElseThrow(
                            () -> new ResourceNotFoundException("CATALOG_DOCUMENT_NOT_FOUND"));
                doc.setDocument(realDocument);
              });
    }

    Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
    return enrollmentMapper.toResponse(savedEnrollment);
  }

  @Transactional(readOnly = true)
  public List<EnrollmentResponse> findAll() {
    return enrollmentRepository.findAll().stream()
        .map(enrollmentMapper::toResponse)
        .collect(Collectors.toList());
  }

  /** Private method to generate the enrollment number: INS-YYYY-XXX */
  private String generateEnrollmentNumber() {
    int currentYear = LocalDate.now().getYear();
    String prefix = "INS-" + currentYear + "-"; // Ex: "INS-2026-"

    Optional<Enrollment> lastEnrollment =
        enrollmentRepository.findTopByEnrollmentNumberStartingWithOrderByEnrollmentNumberDesc(
            prefix);

    int sequence = 1; // We start at 1 by default

    if (lastEnrollment.isPresent()) {
      String lastNumber = lastEnrollment.get().getEnrollmentNumber();
      // We extract the numeric part (everything after "INS-2026-")
      String sequenceStr = lastNumber.substring(prefix.length());
      sequence = Integer.parseInt(sequenceStr) + 1; // We increment
    }

    // String.format("%03d", sequence) forces the format to 3 digits (001, 002... 010... 100)
    return prefix + String.format("%03d", sequence);
  }

  @Transactional(readOnly = true)
  public EnrollmentResponse getById(Long id) {
    Enrollment enrollment =
        enrollmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ENROLLMENT_NOT_FOUND"));

    return enrollmentMapper.toResponse(enrollment);
  }

  @Transactional(readOnly = true)
  public PageResponse<EnrollmentListResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Enrollment> pageResult = enrollmentRepository.findAll(pageable);
    return PageResponse.of(pageResult, enrollmentMapper::toListResponse);
  }
}
