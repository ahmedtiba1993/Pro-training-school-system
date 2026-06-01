package com.tiba.pts.modules.classmanagement.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.service.PdfGeneratorService;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import com.tiba.pts.modules.classmanagement.dto.request.ClassGroupRequest;
import com.tiba.pts.modules.classmanagement.dto.response.ActiveClassGroupResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassGroupDetailResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassGroupResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassManagementStatsResponse;
import com.tiba.pts.modules.classmanagement.mapper.ClassGroupMapper;
import com.tiba.pts.modules.classmanagement.repository.ClassAssignmentRepository;
import com.tiba.pts.modules.classmanagement.repository.ClassGroupRepository;
import com.tiba.pts.modules.documents.repository.EnrollmentDocumentRepository;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassGroupService {

  private final ClassGroupRepository classGroupRepository;
  private final ClassGroupMapper classGroupMapper;
  private final PromotionRepository promotionRepository;
  private final ClassAssignmentRepository classAssignmentRepository;
  private final PdfGeneratorService pdfGeneratorService;
  private final EnrollmentDocumentRepository enrollmentDocumentRepository;

  @Transactional
  public Long createClassGroup(ClassGroupRequest request) {

    // Retrieve Promotion Master Data
    Promotion promotion =
        promotionRepository
            .findById(request.getPromotionId())
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    // The combination [promotion_id + name] must be unique
    if (classGroupRepository.existsByPromotionIdAndNameIgnoreCase(
        request.getPromotionId(), request.getName())) {
      throw new EntityAlreadyExistsException("CLASS_ALREADY_EXISTS_IN_THIS_PROMOTION");
    }

    // Strictly prohibited if the promotion is COMPLETED, ARCHIVED, or CANCELLED
    String promoStatus = promotion.getStatus().toString();
    if ("COMPLETED".equalsIgnoreCase(promoStatus)
        || "ARCHIVED".equalsIgnoreCase(promoStatus)
        || "CANCELLED".equalsIgnoreCase(promoStatus)) {
      throw new BusinessValidationException("PROMOTION_STATUS_FORBIDS_CLASS_CREATION");
    }

    // DTO -> Entity Mapping via MapStruct
    ClassGroup classGroup = classGroupMapper.toEntity(request);

    // RULE: Code (SKU) Generation: {Promotion Code}-{Formatted Class Name}
    // Secure instantiation of the random number generator
    SecureRandom random = new SecureRandom();
    String generatedCode;
    boolean codeExists;

    // Generation and uniqueness check loop (SKU Anti-collision)
    do {
      // Generates a number between 0 and 9999
      int randomNumber = random.nextInt(10000);

      // Formats the code as: CL{PromotionCode}-{4 digits with leading zeros if necessary}
      generatedCode = String.format("CL%s-%04d", promotion.getCode(), randomNumber);

      // Check the existence of the generated SKU in the DB
      codeExists = classGroupRepository.existsByCode(generatedCode);

    } while (codeExists); // If the code already exists, the loop restarts and picks a new number

    // Assign the unique and definitive code to the entity
    classGroup.setCode(generatedCode);

    // The status is strictly forced to DRAFT initially
    classGroup.setStatus(ClassStatus.DRAFT);

    // Set promotion
    classGroup.setPromotion(promotion);

    // Save
    return classGroupRepository.save(classGroup).getId();
  }

  @Transactional(readOnly = true)
  public List<ClassGroupResponse> getAllFiltered(
      Long levelId, Long trainingId, ClassStatus status) {

    // Extract the sorted and filtered list from the DB
    List<ClassGroup> classGroups =
        classGroupRepository.findAllFiltered(trainingId, levelId, status);

    // Map the entity list to the DTO list
    return classGroups.stream().map(classGroupMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ClassManagementStatsResponse getClassManagementStats() {
    // Count active classes
    long activeClasses = classGroupRepository.countByStatus(ClassStatus.ACTIVE);

    // Count students assigned to these active classes
    long assignedStudents = classAssignmentRepository.countByClassGroupStatus(ClassStatus.ACTIVE);

    // Return the consolidated DTO
    return new ClassManagementStatsResponse(activeClasses, assignedStudents);
  }

  @Transactional
  public Long updateStatus(Long id, ClassStatus targetStatus) {
    // Safety validation if the parameter is missing
    if (targetStatus == null) {
      throw new BusinessValidationException("STATUS_REQUIRED");
    }

    // Retrieve aggregate
    ClassGroup classGroup =
        classGroupRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new com.tiba.pts.core.exception.ResourceNotFoundException(
                        "CLASS_GROUP_NOT_FOUND"));

    ClassStatus currentStatus = classGroup.getStatus();

    // If identical, short-circuit
    if (currentStatus == targetStatus) {
      return classGroup.getId();
    }

    // Strict state machine validation
    switch (currentStatus) {
      case ARCHIVED:
        throw new BusinessValidationException("ARCHIVED_CLASS_STATUS_CANNOT_BE_CHANGED");

      case ACTIVE:
        if (targetStatus != ClassStatus.INACTIVE && targetStatus != ClassStatus.ARCHIVED) {
          throw new BusinessValidationException("INVALID_STATUS_TRANSITION_FROM_ACTIVE");
        }
        break;

      case INACTIVE:
        if (targetStatus != ClassStatus.ACTIVE && targetStatus != ClassStatus.ARCHIVED) {
          throw new BusinessValidationException("INVALID_STATUS_TRANSITION_FROM_INACTIVE");
        }
        break;

      case DRAFT:
        break;
    }

    // Mutate and save
    classGroup.setStatus(targetStatus);
    return classGroupRepository.save(classGroup).getId();
  }

  @Transactional(readOnly = true)
  public ClassGroupDetailResponse getDetailById(Long id) {
    // Retrieve class (with eager loading of the Promotion/Training/Level/Specialty tree)
    ClassGroup classGroup =
        classGroupRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new com.tiba.pts.core.exception.ResourceNotFoundException(
                        "CLASS_GROUP_NOT_FOUND"));

    // Direct and ultra-fast mapping to the details DTO
    return classGroupMapper.toDetailResponse(classGroup);
  }

  @Transactional(readOnly = true)
  public List<ActiveClassGroupResponse> getAllActiveClasses() {
    return classGroupMapper.toActiveResponseList(
        classGroupRepository.findAllActiveClassesWithTraining());
  }

  @Transactional(readOnly = true)
  public byte[] exportStudentsPdf(Long classGroupId) {
    // Retrieve class
    ClassGroup classGroup =
        classGroupRepository
            .findById(classGroupId)
            .orElseThrow(
                () ->
                    new com.tiba.pts.core.exception.ResourceNotFoundException(
                        "CLASS_GROUP_NOT_FOUND"));

    // Retrieve assignments with immediate join on Enrollment and Student
    List<com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment> assignments =
        classAssignmentRepository.findByClassGroupId(classGroupId);

    // Extract and alphabetically sort Last Name/First Name structures for the PDF table
    var studentList =
        assignments.stream()
            .map(ca -> ca.getEnrollment().getStudent())
            .map(
                student ->
                    Map.of(
                        "lastName",
                            student
                                .getLastName()
                                .toUpperCase(), // Uppercase last name for professional rendering
                        "firstName", student.getFirstName()))
            .sorted((s1, s2) -> s1.get("lastName").compareTo(s2.get("lastName")))
            .toList();

    // Prepare Thymeleaf variables context
    Map<String, Object> templateVariables =
        Map.of(
            "className", classGroup.getName(),
            "classCode", classGroup.getCode(),
            "students", studentList);

    // Generate the raw table
    return pdfGeneratorService.generatePdf(
        "classmanagement/class-students-list", templateVariables);
  }

  @Transactional(readOnly = true)
  public byte[] exportStudentsWithDocsPdf(Long classGroupId, List<Long> documentIds) {
    // Retrieve class
    ClassGroup classGroup =
        classGroupRepository
            .findById(classGroupId)
            .orElseThrow(() -> new ResourceNotFoundException("CLASS_GROUP_NOT_FOUND"));

    // Retrieve requested documents metadata
    var targetDocuments = enrollmentDocumentRepository.findAllById(documentIds);

    // COMPILATION FIX: Explicit specification of the <String, Object> type for Map.of
    List<Map<String, Object>> headers =
        targetDocuments.stream()
            .map(
                doc -> {
                  String label =
                      doc.getLabel(); // Replace with getName() if your property is called name
                  String shortLabel =
                      (label != null && label.length() >= 2)
                          ? label.substring(0, 2).toUpperCase()
                          : label;

                  return Map.<String, Object>of(
                      "id",
                      doc.getId(),
                      "shortLabel",
                      shortLabel,
                      "fullLabel",
                      label != null ? label : "");
                })
            .toList();

    // Retrieve students with optimized EntityGraph
    List<ClassAssignment> assignments =
        classAssignmentRepository.findWithSubmissionsByClassGroupId(classGroupId);

    // Construct table rows (Full Name + array of "X" or "" statuses)
    List<Map<String, Object>> studentRows =
        assignments.stream()
            .map(
                ca -> {
                  Enrollment env = ca.getEnrollment();
                  var student = env.getStudent();
                  String fullName =
                      student.getFirstName().toUpperCase() + " " + student.getLastName();

                  // For each requested document, check if it is provided (isProvided == true)
                  List<String> docStatuses =
                      documentIds.stream()
                          .map(
                              docId -> {
                                boolean isProvided =
                                    env.getEnrollmentDocumentSubmissions().stream()
                                        .anyMatch(
                                            sub ->
                                                sub.getDocument().getId().equals(docId)
                                                    && sub.isProvided());
                                return isProvided ? "X" : "";
                              })
                          .toList();

                  return Map.<String, Object>of(
                      "fullName", fullName,
                      "statuses", docStatuses);
                })
            .sorted(
                (s1, s2) -> ((String) s1.get("fullName")).compareTo((String) s2.get("fullName")))
            .toList();

    // Inject data into the Thymeleaf context
    Map<String, Object> templateVariables =
        Map.of(
            "className",
            classGroup.getName(),
            "classCode",
            classGroup.getCode(),
            "headers",
            headers,
            "students",
            studentRows);

    // Generate via PDF engine
    return pdfGeneratorService.generatePdf(
        "classmanagement/class-students-docs-list", templateVariables);
  }
}
