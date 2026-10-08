package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.mapper.ExamSessionMapper;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamSessionServiceDeleteTest {

  @Mock
  private ExamSessionRepository examSessionRepository;

  @Mock
  private PeriodRepository periodRepository;

  @Mock
  private ExamSessionMapper examSessionMapper;

  @InjectMocks
  private ExamSessionService examSessionService;

  @Test
  @DisplayName("Should successfully delete RETAKE session when unlocked and parent year is PLANNED")
  void deleteExamSession_success_retake() {
    Long sessionId = 1L;
    AcademicYear year = AcademicYear.builder().status(YearStatus.PLANNED).build();
    Period period = Period.builder().id(10L).academicYear(year).build();
    ExamSession session = ExamSession.builder()
        .id(sessionId)
        .sessionType(SessionType.RETAKE)
        .isLocked(false)
        .period(period)
        .build();

    when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    examSessionService.deleteExamSession(sessionId);

    verify(examSessionRepository).delete(session);
  }

  @Test
  @DisplayName("Should successfully delete MAIN session when no RETAKE exists")
  void deleteExamSession_success_main() {
    Long sessionId = 1L;
    Long periodId = 10L;
    AcademicYear year = AcademicYear.builder().status(YearStatus.PLANNED).build();
    Period period = Period.builder().id(periodId).academicYear(year).build();
    ExamSession session = ExamSession.builder()
        .id(sessionId)
        .sessionType(SessionType.MAIN)
        .isLocked(false)
        .period(period)
        .build();

    when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(examSessionRepository.existsByPeriodIdAndSessionType(periodId, SessionType.RETAKE)).thenReturn(false);

    examSessionService.deleteExamSession(sessionId);

    verify(examSessionRepository).delete(session);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when session not found")
  void deleteExamSession_notFound() {
    Long sessionId = 99L;
    when(examSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> examSessionService.deleteExamSession(sessionId)
    );

    assertEquals("ACADEMIC_EXAM_SESSION_NOT_FOUND", ex.getMessage());
    verify(examSessionRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when session is locked")
  void deleteExamSession_locked() {
    Long sessionId = 1L;
    ExamSession session = ExamSession.builder()
        .id(sessionId)
        .isLocked(true)
        .build();

    when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> examSessionService.deleteExamSession(sessionId)
    );

    assertEquals("ACADEMIC_EXAM_SESSION_CANNOT_BE_DELETED_LOCKED", ex.getMessage());
    verify(examSessionRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when parent year is not PLANNED")
  void deleteExamSession_yearNotPlanned() {
    Long sessionId = 1L;
    AcademicYear year = AcademicYear.builder().status(YearStatus.IN_PROGRESS).build();
    Period period = Period.builder().id(10L).academicYear(year).build();
    ExamSession session = ExamSession.builder()
        .id(sessionId)
        .isLocked(false)
        .period(period)
        .build();

    when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> examSessionService.deleteExamSession(sessionId)
    );

    assertEquals("ACADEMIC_EXAM_SESSION_CANNOT_BE_DELETED_NOT_PLANNED", ex.getMessage());
    verify(examSessionRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when deleting MAIN session while RETAKE exists")
  void deleteExamSession_mainWithRetake() {
    Long sessionId = 1L;
    Long periodId = 10L;
    AcademicYear year = AcademicYear.builder().status(YearStatus.PLANNED).build();
    Period period = Period.builder().id(periodId).academicYear(year).build();
    ExamSession session = ExamSession.builder()
        .id(sessionId)
        .sessionType(SessionType.MAIN)
        .isLocked(false)
        .period(period)
        .build();

    when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(examSessionRepository.existsByPeriodIdAndSessionType(periodId, SessionType.RETAKE)).thenReturn(true);

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> examSessionService.deleteExamSession(sessionId)
    );

    assertEquals("ACADEMIC_EXAM_SESSION_CANNOT_DELETE_MAIN_WITH_RETAKE", ex.getMessage());
    verify(examSessionRepository, never()).delete(any());
  }
}
