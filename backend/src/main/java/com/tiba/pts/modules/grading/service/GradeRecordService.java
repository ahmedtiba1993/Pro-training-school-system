package com.tiba.pts.modules.grading.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.entity.GradeRecord;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.dto.request.GradeRecordRequest;
import com.tiba.pts.modules.grading.dto.response.GradeRecordResponse;
import com.tiba.pts.modules.grading.mapper.GradeRecordMapper;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import com.tiba.pts.modules.grading.repository.GradeRecordRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeRecordService {

  private final AssessmentRepository assessmentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final GradeRecordRepository gradeRecordRepository;
  private final GradeRecordMapper gradeRecordMapper;

  @Transactional(readOnly = true)
  public List<GradeRecordResponse> getAllGradesByAssessment(Long assessmentId) {
    Assessment assessment =
        assessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    Long promotionId =
        assessment.getPromotionSubject() != null
                && assessment.getPromotionSubject().getPromotion() != null
            ? assessment.getPromotionSubject().getPromotion().getId()
            : null;

    if (promotionId == null) {
      return List.of();
    }

    List<Enrollment> enrollments =
        enrollmentRepository.findActiveEnrollmentsByPromotionId(promotionId);

    List<GradeRecord> gradeRecords =
        gradeRecordRepository.findByAssessmentIdWithStudent(assessmentId);

    Map<Long, GradeRecord> gradesByEnrollmentId =
        gradeRecords.stream()
            .collect(
                Collectors.toMap(
                    gr -> gr.getEnrollment().getId(),
                    gr -> gr,
                    (existing, replacement) -> existing));

    return enrollments.stream()
        .map(e -> gradeRecordMapper.toResponse(e, gradesByEnrollmentId.get(e.getId())))
        .toList();
  }

  @Transactional
  public void addGradeRecordsForAssessment(Long assessmentId, List<GradeRecordRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new BusinessValidationException("GRADES_LIST_REQUIRED");
    }

    // Validate assessment
    Assessment assessment =
        assessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() == AssessmentStatus.PLANNED
        || assessment.getStatus() == AssessmentStatus.DRAFT) {
      throw new BusinessValidationException("ASSESSMENT_NOT_READY_FOR_GRADING");
    }
    if (assessment.getStatus() == AssessmentStatus.SUBMITTED_TO_ADMIN
        || assessment.getStatus() == AssessmentStatus.PUBLISHED
        || assessment.getStatus() == AssessmentStatus.LOCKED) {
      throw new BusinessValidationException("ASSESSMENT_GRADING_CLOSED");
    }
    if (assessment.getStatus() != AssessmentStatus.GRADING_IN_PROGRESS) {
      throw new BusinessValidationException("ASSESSMENT_INVALID_STATUS_FOR_GRADING");
    }

    Promotion assessmentPromotion =
        assessment.getPromotionSubject() != null
            ? assessment.getPromotionSubject().getPromotion()
            : null;

    // Index existing grades for this assessment
    List<GradeRecord> existingGrades =
        gradeRecordRepository.findByAssessmentIdWithStudent(assessmentId);
    Map<Long, GradeRecord> gradeMap =
        existingGrades.stream()
            .collect(
                Collectors.toMap(
                    gr -> gr.getEnrollment().getId(),
                    gr -> gr,
                    (existing, replacement) -> existing));

    List<GradeRecord> toSave = new ArrayList<>();

    // Process each grade
    for (GradeRecordRequest req : requests) {
      Enrollment enrollment =
          enrollmentRepository
              .findById(req.enrollmentId())
              .orElseThrow(() -> new ResourceNotFoundException("ENROLLMENT_NOT_FOUND"));

      Promotion enrollmentPromotion = enrollment.getPromotion();
      if (assessmentPromotion == null
          || enrollmentPromotion == null
          || !Objects.equals(assessmentPromotion.getId(), enrollmentPromotion.getId())) {
        throw new BusinessValidationException("ENROLLMENT_NOT_IN_ASSESSMENT_PROMOTION");
      }

      if (enrollment.getStatus() != EnrollmentStatus.VALIDATED
          && enrollment.getStatus() != EnrollmentStatus.CONDITIONALLY_VALIDATED) {
        throw new BusinessValidationException("ENROLLMENT_STATUS_INVALID_FOR_GRADING");
      }

      BigDecimal finalScore = req.score();
      switch (req.attendanceStatus()) {
        case PRESENT -> {
          if (finalScore == null) {
            throw new BusinessValidationException("SCORE_REQUIRED_FOR_PRESENT_STUDENT");
          }
          if (finalScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException("SCORE_CANNOT_BE_NEGATIVE");
          }
          if (assessment.getTotalMarks() != null
              && finalScore.compareTo(BigDecimal.valueOf(assessment.getTotalMarks())) > 0) {
            throw new BusinessValidationException("SCORE_EXCEEDS_TOTAL_MARKS");
          }
        }
        case ABSENT_JUSTIFIED -> finalScore = null;
        case ABSENT_UNJUSTIFIED -> finalScore = BigDecimal.ZERO;
      }

      GradeRecord record =
          gradeMap.computeIfAbsent(
              enrollment.getId(),
              k ->
                  GradeRecord.builder()
                      .assessment(assessment)
                      .enrollment(enrollment)
                      .build());

      record.setScore(finalScore);
      record.setAttendanceStatus(req.attendanceStatus());
      record.setTeacherComment(req.teacherComment());
      record.setIsOverridden(Boolean.FALSE);
      record.setOverrideReason(null);

      toSave.add(record);
    }

    gradeRecordRepository.saveAll(toSave);
  }

  @Transactional
  public Long addGradeRecord(GradeRecordRequest request) {
    addGradeRecordsForAssessment(request.assessmentId(), List.of(request));
    return gradeRecordRepository
        .findByAssessmentIdAndEnrollmentId(request.assessmentId(), request.enrollmentId())
        .map(GradeRecord::getId)
        .orElse(null);
  }
}
