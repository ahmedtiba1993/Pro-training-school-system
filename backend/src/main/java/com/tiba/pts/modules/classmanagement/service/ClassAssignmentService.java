package com.tiba.pts.modules.classmanagement.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import com.tiba.pts.modules.classmanagement.dto.request.ClassAssignmentRequest;
import com.tiba.pts.modules.classmanagement.dto.response.ClassStudentResponse;
import com.tiba.pts.modules.classmanagement.mapper.ClassAssignmentMapper;
import com.tiba.pts.modules.classmanagement.repository.ClassAssignmentRepository;
import com.tiba.pts.modules.classmanagement.repository.ClassGroupRepository;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassAssignmentService {

  private final ClassAssignmentRepository classAssignmentRepository;
  private final ClassGroupRepository classGroupRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final ClassAssignmentMapper classAssignmentMapper;

  @Transactional
  public Long assignStudent(ClassAssignmentRequest request) {

    ClassGroup classGroup =
        classGroupRepository
            .findById(request.getClassGroupId())
            .orElseThrow(() -> new ResourceNotFoundException("CLASS_GROUP_NOT_FOUND"));

    // The class must be in ACTIVE status
    if (classGroup.getStatus() != ClassStatus.ACTIVE) {
      throw new BusinessValidationException("CLASS_STATUS_MUST_BE_ACTIVE");
    }

    // The couple [enrollment_id + class_group_id] must be unique
    if (classAssignmentRepository.existsByClassGroupIdAndEnrollmentId(
        request.getClassGroupId(), request.getEnrollmentId())) {
      throw new EntityAlreadyExistsException("STUDENT_ALREADY_ASSIGNED_TO_THIS_CLASS");
    }

    // Loading the student's enrollment
    Enrollment enrollment =
        enrollmentRepository
            .findById(request.getEnrollmentId())
            .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND"));

    // The enrollment must be in VALIDATED or CONDITIONALLY_VALIDATED status
    String enrollmentStatus = enrollment.getStatus().toString();
    if (!"VALIDATED".equals(enrollmentStatus)
        && !"CONDITIONALLY_VALIDATED".equals(enrollmentStatus)) {
      throw new BusinessValidationException("ENROLLMENT_STATUS_INVALID_FOR_ASSIGNMENT");
    }

    // Promotion consistency (enrollment.promotion_id == classGroup.promotion_id)
    if (!classGroup.getPromotion().getId().equals(enrollment.getPromotion().getId())) {
      throw new BusinessValidationException("PROMOTION_MISMATCH_BETWEEN_CLASS_AND_ENROLLMENT");
    }

    // The number of current assignments (currentSize) must be strictly less than the
    // capacity
    if (classGroup.getCurrentSize() >= classGroup.getCapacity()) {
      throw new BusinessValidationException("CLASS_MAXIMUM_CAPACITY_EXCEEDED");
    }

    // Mapping and persistence of the assignment
    ClassAssignment classAssignment = classAssignmentMapper.toEntity(request);

    // Using the Domain Helper to maintain the consistency of the aggregation tree
    classAssignment.setClassGroup(classGroup);
    classAssignment.setEnrollment(enrollment);

    return classAssignmentRepository.save(classAssignment).getId();
  }

  @Transactional(readOnly = true)
  public List<ClassStudentResponse> getStudentsByClassGroup(Long classGroupId) {

    // Security validation: Does the requested class exist
    if (!classGroupRepository.existsById(classGroupId)) {
      throw new com.tiba.pts.core.exception.ResourceNotFoundException("CLASS_GROUP_NOT_FOUND");
    }

    // Optimized retrieval (without N+1 loop queries)
    List<ClassAssignment> assignments = classAssignmentRepository.findByClassGroupId(classGroupId);

    // Transformation of the entity stream into a list of DTOs
    return assignments.stream().map(classAssignmentMapper::toStudentResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ClassStudentResponse> getActiveStudentsByClassGroup(Long classGroupId) {

    // Security validation: Does the requested class exist
    if (!classGroupRepository.existsById(classGroupId)) {
      throw new com.tiba.pts.core.exception.ResourceNotFoundException("CLASS_GROUP_NOT_FOUND");
    }

    // Optimized retrieval (without N+1 loop queries)
    List<ClassAssignment> assignments =
        classAssignmentRepository.findByClassGroupIdAndEnrollmentStatusIn(
            classGroupId,
            List.of(EnrollmentStatus.VALIDATED, EnrollmentStatus.CONDITIONALLY_VALIDATED));

    // Transformation of the entity stream into a list of DTOs
    return assignments.stream().map(classAssignmentMapper::toStudentResponse).toList();
  }
}
