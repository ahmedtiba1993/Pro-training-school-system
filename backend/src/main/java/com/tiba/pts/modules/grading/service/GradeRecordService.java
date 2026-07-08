package com.tiba.pts.modules.grading.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.entity.GradeRecord;
import com.tiba.pts.modules.grading.domain.enums.AttendanceStatus;
import com.tiba.pts.modules.grading.dto.request.AssessmentGradesRequest;
import com.tiba.pts.modules.grading.dto.request.GradeRecordInput;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import com.tiba.pts.modules.grading.repository.GradeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GradeRecordService {

  private final AssessmentRepository assessmentRepository;
  private final GradeRecordRepository gradeRecordRepository;
  private final EnrollmentRepository enrollmentRepository;

  @Transactional
  public void saveGrades(AssessmentGradesRequest request) {
    Assessment assessment =
        assessmentRepository
            .findById(request.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    for (GradeRecordInput input : request.grades()) {
      BigDecimal score = input.score();
      if (input.attendanceStatus() == AttendanceStatus.ABSENT_UNJUSTIFIED) {
        score = BigDecimal.ZERO;
      } else if (input.attendanceStatus() == AttendanceStatus.ABSENT_JUSTIFIED) {
        score = null;
      }

      Enrollment enrollment =
          enrollmentRepository
              .findById(input.enrollmentId())
              .orElseThrow(() -> new ResourceNotFoundException("ENROLLMENT_NOT_FOUND"));

      if (enrollment.getStatus() == EnrollmentStatus.DROPPED_OUT ||
          enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
        score = null;
      }

      if (score != null && assessment.getTotalMarks() != null) {
        BigDecimal totalMarks = BigDecimal.valueOf(assessment.getTotalMarks());
        if (score.compareTo(totalMarks) > 0) {
          throw new BusinessValidationException("SCORE_EXCEEDS_TOTAL_MARKS");
        }
      }

      GradeRecord gradeRecord =
          gradeRecordRepository
              .findByAssessmentIdAndEnrollmentId(assessment.getId(), enrollment.getId())
              .orElseGet(
                  () ->
                      GradeRecord.builder().assessment(assessment).enrollment(enrollment).build());

      gradeRecord.setScore(score);
      gradeRecord.setAttendanceStatus(input.attendanceStatus());
      gradeRecord.setTeacherComment(input.teacherComment());

      gradeRecordRepository.save(gradeRecord);
    }
  }
}
