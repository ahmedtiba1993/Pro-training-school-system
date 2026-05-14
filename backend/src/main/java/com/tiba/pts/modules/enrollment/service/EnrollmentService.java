package com.tiba.pts.modules.enrollment.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.service.PdfGeneratorService;
import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.documents.repository.EnrollmentDocumentRepository;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.entity.EnrollmentDocumentSubmission;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentRequest;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentSearchRequest;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentListResponse;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.mapper.EnrollmentMapper;
import com.tiba.pts.modules.enrollment.repository.EnrollmentDocumentSubmissionRepository;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.profiles.domain.entity.Parent;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.domain.entity.StudentParent;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.repository.StudentRepository;
import com.tiba.pts.modules.profiles.service.StudentService;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.entity.Role;
import com.tiba.pts.modules.user.entity.User;
import com.tiba.pts.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final EnrollmentDocumentSubmissionRepository submissionRepository;
  private final PdfGeneratorService pdfGeneratorService;
  private final UserService userService;

  @Transactional
  public EnrollmentResponse create(EnrollmentRequest request) {

    // Promotion validation
    Promotion promotion =
        promotionRepository
            .findById(request.getPromotionId())
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    Student student = new Student();
    if (request.getExistingStudentId() != null) {
      student =
          studentRepository
              .findById(request.getExistingStudentId())
              .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND"));

      // --- VERIFICATION ---
      boolean isAlreadyEnrolled =
          enrollmentRepository.existsByStudentIdAndPromotionId(student.getId(), promotion.getId());

      if (isAlreadyEnrolled) {
        // Use the appropriate exception for your project (BadRequestException,
        // DuplicateResourceException, etc.)
        throw new BusinessValidationException("STUDENT_ALREADY_ENROLLED_IN_PROMOTION");
      }
    } else {
      // Student creation (Student + Siblings + Parents) via the Profiles module
      StudentRequest profileRequest = enrollmentMapper.toStudentRequest(request.getStudentInfo());
      Long studentId = studentService.createStudent(profileRequest);
      student = studentRepository.getReferenceById(studentId);
    }

    // Enrollment process continuation
    Enrollment enrollment = enrollmentMapper.toEntity(request);
    enrollment.setStudent(student);
    enrollment.setPromotion(promotion);
    enrollment.setEnrollmentNumber(generateEnrollmentNumber() + student.getId());
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
    String prefix = "INS" + currentYear; // Ex: "INS-2026-"

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

    // String.format("%03d", sequence) forces the format to 4 digits (0001, 0002... 0010... 0100)
    return prefix + String.format("%04d", sequence);
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
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
    Page<Enrollment> pageResult = enrollmentRepository.findAll(pageable);
    return PageResponse.of(pageResult, enrollmentMapper::toListResponse);
  }

  @Transactional(readOnly = true)
  public PageResponse<EnrollmentListResponse> getAllPaged(EnrollmentSearchRequest searchParams) {

    // Using DTO default values
    Pageable pageable =
        PageRequest.of(
            searchParams.getPage(),
            searchParams.getSize(),
            Sort.by(Sort.Direction.DESC, "createdDate"));

    // Keyword cleanup
    String cleanKeyword =
        (searchParams.getKeyword() != null && !searchParams.getKeyword().trim().isEmpty())
            ? searchParams.getKeyword().trim()
            : null;

    Page<Enrollment> pageResult =
        enrollmentRepository.findAllWithFilters(
            cleanKeyword,
            searchParams.getLevelId(),
            searchParams.getSpecialtyId(),
            searchParams.getPromotionId(),
            searchParams.getStatus(),
            pageable);

    return PageResponse.of(pageResult, enrollmentMapper::toListResponse);
  }

  @Transactional
  public void toggleDocumentStatus(Long submissionId) {
    EnrollmentDocumentSubmission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_SUBMISSION_NOT_FOUND"));

    // Toggle status
    submission.setProvided(!submission.isProvided());

    submissionRepository.save(submission);
  }

  @Transactional(readOnly = true)
  public byte[] exportEnrollmentToPdf(Long enrollmentId) {
    Enrollment enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("ENROLLMENT_NOT_FOUND"));

    Map<String, Object> variables = new HashMap<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Context hydration for Thymeleaf
    variables.put("enrollmentNumber", enrollment.getEnrollmentNumber());
    variables.put(
        "enrollmentDate",
        enrollment.getCreatedDate() != null
            ? enrollment.getCreatedDate().format(formatter)
            : LocalDate.now().format(formatter));
    variables.put(
        "studentName",
        enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName());
    variables.put("studentMatricule", enrollment.getStudent().getStudentCode());

    variables.put("levelName", enrollment.getPromotion().getTraining().getLevel().getLabel());
    variables.put(
        "specialtyName", enrollment.getPromotion().getTraining().getSpecialty().getLabel());
    variables.put("promotionName", enrollment.getPromotion().getName());
    variables.put(
        "trainingType", enrollment.getPromotion().getTraining().getTrainingType().getLabelFr());
    // ---- DYNAMIC USERNAME RETRIEVAL ----
    // Search for the user linked to this Student (Person).
    // If the account hasn't been created yet, display "NON_DEFINI" (or your preference).
    String username =
        userService
            .findByPersonId(enrollment.getStudent().getId())
            .map(User::getUsername)
            .orElse("NON_DEFINI");

    variables.put("username", username);
    variables.put("currentDate", LocalDate.now().format(formatter));

    return pdfGeneratorService.generatePdf("enrollment-fiche", variables);
  }

  @Transactional(readOnly = true)
  public boolean checkStudentEnrollmentExistence(Long studentId, Long promotionId) {
    return enrollmentRepository.existsByStudentIdAndPromotionId(studentId, promotionId);
  }

  @Transactional
  public void updateStatus(Long id, EnrollmentStatus newStatus) {
    Enrollment enrollment =
        enrollmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ENROLLMENT_NOT_FOUND"));

    EnrollmentStatus currentStatus = enrollment.getStatus();

    // Prevent update if the status is already the same
    if (currentStatus == newStatus) {
      throw new BusinessValidationException("STATUS_ALREADY_SET");
    }

    // Verification of business transition rules
    if (!isValidTransition(currentStatus, newStatus)) {
      throw new BusinessValidationException("INVALID_STATUS_TRANSITION");
    }

    enrollment.setStatus(newStatus);
    enrollmentRepository.save(enrollment);

    // --- ACCOUNT CREATION IF ENROLLMENT IS VALIDATED (TOTAL OR CONDITIONAL) ---
    if (newStatus == EnrollmentStatus.CONDITIONALLY_VALIDATED
        || newStatus == EnrollmentStatus.VALIDATED) {
      createStudentAccountSafe(enrollment.getStudent());
      createParentAccountsSafe(enrollment.getStudent());
    }
  }

  /** Private method to validate the state machine rules for EnrollmentStatus. */
  private boolean isValidTransition(EnrollmentStatus currentStatus, EnrollmentStatus newStatus) {
    return switch (currentStatus) {
      case PRE_ENROLLED ->
          List.of(
                  EnrollmentStatus.INCOMPLETE,
                  EnrollmentStatus.WAITLISTED,
                  EnrollmentStatus.CONDITIONALLY_VALIDATED,
                  EnrollmentStatus.VALIDATED,
                  EnrollmentStatus.REJECTED,
                  EnrollmentStatus.CANCELLED)
              .contains(newStatus);

      case INCOMPLETE ->
          List.of(
                  EnrollmentStatus.CONDITIONALLY_VALIDATED,
                  EnrollmentStatus.VALIDATED,
                  EnrollmentStatus.WAITLISTED,
                  EnrollmentStatus.CANCELLED)
              .contains(newStatus);

      case WAITLISTED ->
          List.of(EnrollmentStatus.CONDITIONALLY_VALIDATED, EnrollmentStatus.VALIDATED)
              .contains(newStatus);

      case CONDITIONALLY_VALIDATED ->
          List.of(
                  EnrollmentStatus.VALIDATED,
                  EnrollmentStatus.SUSPENDED,
                  EnrollmentStatus.DROPPED_OUT)
              .contains(newStatus);

      case VALIDATED ->
          List.of(
                  EnrollmentStatus.SUSPENDED,
                  EnrollmentStatus.DROPPED_OUT,
                  EnrollmentStatus.COMPLETED)
              .contains(newStatus);

      case SUSPENDED -> newStatus == EnrollmentStatus.VALIDATED;

      // Final states (Terminal states): no transition possible
      case REJECTED, CANCELLED, DROPPED_OUT, COMPLETED -> false;
    };
  }

  private void createStudentAccountSafe(Student student) {
    if (student == null || student.getStudentCode() == null) {
      return;
    }

    String username = student.getStudentCode();

    // Check BEFORE calling creation to avoid transaction Rollback
    if (userService.existsByUsername(username)) {
      return;
    }

    UserCreateRequest accountRequest =
        UserCreateRequest.builder()
            .username(username)
            .password("12345678")
            .role(Role.ROLE_STUDENT)
            .personId(student.getId())
            .build();

    userService.createUser(accountRequest);
  }

  /** Private method to safely create parent accounts using their phone number. */
  private void createParentAccountsSafe(Student student) {
    if (student == null || student.getParents() == null || student.getParents().isEmpty()) {
      return;
    }

    for (StudentParent studentParent : student.getParents()) {
      Parent parent = studentParent.getParent();

      if (parent != null && parent.getPhone() != null && !parent.getPhone().trim().isEmpty()) {

        String phoneAsUsername = parent.getPhone().trim();

        // Check BEFORE calling creation to avoid transaction Rollback
        if (userService.existsByUsername(phoneAsUsername)) {
          continue; // Move to the next parent
        }

        UserCreateRequest accountRequest =
            UserCreateRequest.builder()
                .username(phoneAsUsername)
                .password("12345678")
                .role(Role.ROLE_PARENT)
                .personId(parent.getId())
                .build();

        userService.createUser(accountRequest);
      }
    }
  }
}
