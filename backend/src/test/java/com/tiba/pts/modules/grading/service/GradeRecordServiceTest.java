package com.tiba.pts.modules.grading.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.entity.GradeRecord;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AttendanceStatus;
import com.tiba.pts.modules.grading.dto.request.GradeRecordRequest;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import com.tiba.pts.modules.grading.repository.GradeRecordRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeRecordServiceTest {

  @Mock private AssessmentRepository assessmentRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private GradeRecordRepository gradeRecordRepository;

  @InjectMocks private GradeRecordService gradeRecordService;

  private Assessment assessment;
  private Enrollment enrollment;
  private Promotion promotion;

  @BeforeEach
  void setUp() {
    AccreditedPromotion promo = new AccreditedPromotion();
    promo.setId(10L);
    promo.setName("Promotion Test");
    promotion = promo;

    PromotionSubject promotionSubject =
        PromotionSubject.builder().id(20L).promotion(promotion).build();

    assessment =
        Assessment.builder()
            .id(1L)
            .title("Examen Java")
            .totalMarks(20.0)
            .status(AssessmentStatus.GRADING_IN_PROGRESS)
            .promotionSubject(promotionSubject)
            .build();

    enrollment =
        Enrollment.builder()
            .id(2L)
            .promotion(promotion)
            .status(EnrollmentStatus.VALIDATED)
            .build();
  }

  @Test
  void addGradeRecord_throwsWhenAssessmentNotFound() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.empty());

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, BigDecimal.TEN, AttendanceStatus.PRESENT, null, false, null);

    ResourceNotFoundException ex =
        assertThrows(
            ResourceNotFoundException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("ASSESSMENT_NOT_FOUND", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenAssessmentPlannedOrDraft() {
    assessment.setStatus(AssessmentStatus.PLANNED);
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, BigDecimal.TEN, AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("ASSESSMENT_NOT_READY_FOR_GRADING", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenAssessmentClosed() {
    assessment.setStatus(AssessmentStatus.PUBLISHED);
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, BigDecimal.TEN, AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("ASSESSMENT_GRADING_CLOSED", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenEnrollmentNotFound() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.empty());

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, BigDecimal.TEN, AttendanceStatus.PRESENT, null, false, null);

    ResourceNotFoundException ex =
        assertThrows(
            ResourceNotFoundException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("ENROLLMENT_NOT_FOUND", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenPromotionMismatch() {
    AccreditedPromotion otherPromotion = new AccreditedPromotion();
    otherPromotion.setId(999L);
    otherPromotion.setName("Other Promotion");
    enrollment.setPromotion(otherPromotion);

    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, BigDecimal.TEN, AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("ENROLLMENT_NOT_IN_ASSESSMENT_PROMOTION", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenEnrollmentStatusInvalid() {
    enrollment.setStatus(EnrollmentStatus.SUSPENDED);

    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, BigDecimal.TEN, AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("ENROLLMENT_STATUS_INVALID_FOR_GRADING", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenPresentAndScoreNull() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));

    GradeRecordRequest request =
        new GradeRecordRequest(1L, 2L, null, AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("SCORE_REQUIRED_FOR_PRESENT_STUDENT", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenPresentAndScoreNegative() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));

    GradeRecordRequest request =
        new GradeRecordRequest(
            1L, 2L, BigDecimal.valueOf(-1.0), AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("SCORE_CANNOT_BE_NEGATIVE", ex.getMessage());
  }

  @Test
  void addGradeRecord_throwsWhenScoreExceedsTotalMarks() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));

    GradeRecordRequest request =
        new GradeRecordRequest(
            1L, 2L, BigDecimal.valueOf(20.5), AttendanceStatus.PRESENT, null, false, null);

    BusinessValidationException ex =
        assertThrows(
            BusinessValidationException.class, () -> gradeRecordService.addGradeRecord(request));
    assertEquals("SCORE_EXCEEDS_TOTAL_MARKS", ex.getMessage());
  }

  @Test
  void addGradeRecord_successPresentStudent() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));
    when(gradeRecordRepository.findByAssessmentIdAndEnrollmentId(1L, 2L))
        .thenReturn(Optional.empty());

    GradeRecord savedEntity = GradeRecord.builder().id(100L).build();
    when(gradeRecordRepository.save(any(GradeRecord.class))).thenReturn(savedEntity);

    GradeRecordRequest request =
        new GradeRecordRequest(
            1L, 2L, BigDecimal.valueOf(15.5), AttendanceStatus.PRESENT, "Good job", true, "Unauthorized reason");

    Long savedId = gradeRecordService.addGradeRecord(request);

    assertEquals(100L, savedId);

    ArgumentCaptor<GradeRecord> captor = ArgumentCaptor.forClass(GradeRecord.class);
    verify(gradeRecordRepository).save(captor.capture());

    GradeRecord captured = captor.getValue();
    assertEquals(BigDecimal.valueOf(15.5), captured.getScore());
    assertEquals(AttendanceStatus.PRESENT, captured.getAttendanceStatus());
    assertEquals("Good job", captured.getTeacherComment());
    assertFalse(captured.getIsOverridden());
    assertNull(captured.getOverrideReason());
  }

  @Test
  void addGradeRecord_forcesScoreNullWhenAbsentJustified() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));
    when(gradeRecordRepository.findByAssessmentIdAndEnrollmentId(1L, 2L))
        .thenReturn(Optional.empty());

    GradeRecord savedEntity = GradeRecord.builder().id(101L).build();
    when(gradeRecordRepository.save(any(GradeRecord.class))).thenReturn(savedEntity);

    GradeRecordRequest request =
        new GradeRecordRequest(
            1L, 2L, BigDecimal.valueOf(10.0), AttendanceStatus.ABSENT_JUSTIFIED, "Medical certificate", false, null);

    Long savedId = gradeRecordService.addGradeRecord(request);

    assertEquals(101L, savedId);

    ArgumentCaptor<GradeRecord> captor = ArgumentCaptor.forClass(GradeRecord.class);
    verify(gradeRecordRepository).save(captor.capture());

    GradeRecord captured = captor.getValue();
    assertNull(captured.getScore());
    assertEquals(AttendanceStatus.ABSENT_JUSTIFIED, captured.getAttendanceStatus());
    assertFalse(captured.getIsOverridden());
    assertNull(captured.getOverrideReason());
  }

  @Test
  void addGradeRecord_forcesScoreZeroWhenAbsentUnjustified() {
    when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
    when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(enrollment));
    when(gradeRecordRepository.findByAssessmentIdAndEnrollmentId(1L, 2L))
        .thenReturn(Optional.empty());

    GradeRecord savedEntity = GradeRecord.builder().id(102L).build();
    when(gradeRecordRepository.save(any(GradeRecord.class))).thenReturn(savedEntity);

    GradeRecordRequest request =
        new GradeRecordRequest(
            1L, 2L, BigDecimal.valueOf(14.0), AttendanceStatus.ABSENT_UNJUSTIFIED, null, false, null);

    Long savedId = gradeRecordService.addGradeRecord(request);

    assertEquals(102L, savedId);

    ArgumentCaptor<GradeRecord> captor = ArgumentCaptor.forClass(GradeRecord.class);
    verify(gradeRecordRepository).save(captor.capture());

    GradeRecord captured = captor.getValue();
    assertEquals(BigDecimal.ZERO, captured.getScore());
    assertEquals(AttendanceStatus.ABSENT_UNJUSTIFIED, captured.getAttendanceStatus());
    assertFalse(captured.getIsOverridden());
    assertNull(captured.getOverrideReason());
  }
}
