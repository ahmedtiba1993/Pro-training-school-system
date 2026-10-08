package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.mapper.PeriodMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodServiceDeleteTest {

  @Mock
  private PeriodRepository periodRepository;

  @Mock
  private AcademicYearRepository academicYearRepository;

  @Mock
  private PeriodMapper periodMapper;

  @Mock
  private PromotionRepository promotionRepository;

  @Mock
  private ExamSessionRepository examSessionRepository;

  @InjectMocks
  private PeriodService periodService;

  @Test
  @DisplayName("Should successfully delete period and decrement orderIndex of subsequent periods")
  void deletePeriod_success() {
    Long periodId = 2L;
    Long yearId = 10L;

    AcademicYear year = AcademicYear.builder()
        .id(yearId)
        .status(YearStatus.PLANNED)
        .build();

    Period periodToDelete = Period.builder()
        .id(periodId)
        .orderIndex(2)
        .isLocked(false)
        .academicYear(year)
        .build();

    Period subsequentPeriod = Period.builder()
        .id(3L)
        .orderIndex(3)
        .isLocked(false)
        .academicYear(year)
        .build();

    when(periodRepository.findById(periodId)).thenReturn(Optional.of(periodToDelete));
    when(examSessionRepository.existsByPeriodId(periodId)).thenReturn(false);
    when(periodRepository.findByAcademicYearIdAndOrderIndexGreaterThanOrderByOrderIndexAsc(yearId, 2))
        .thenReturn(List.of(subsequentPeriod));

    periodService.deletePeriod(periodId);

    verify(periodRepository).delete(periodToDelete);
    assertEquals(2, subsequentPeriod.getOrderIndex());
    verify(periodRepository).saveAll(List.of(subsequentPeriod));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when period not found")
  void deletePeriod_notFound() {
    Long periodId = 99L;
    when(periodRepository.findById(periodId)).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> periodService.deletePeriod(periodId)
    );

    assertEquals("ACADEMIC_PERIOD_NOT_FOUND", ex.getMessage());
    verify(periodRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when period is locked")
  void deletePeriod_locked() {
    Long periodId = 1L;
    Period period = Period.builder()
        .id(periodId)
        .isLocked(true)
        .build();

    when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> periodService.deletePeriod(periodId)
    );

    assertEquals("ACADEMIC_PERIOD_CANNOT_BE_DELETED_LOCKED", ex.getMessage());
    verify(periodRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when parent year is not PLANNED")
  void deletePeriod_parentYearNotPlanned() {
    Long periodId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(10L)
        .status(YearStatus.IN_PROGRESS)
        .build();

    Period period = Period.builder()
        .id(periodId)
        .isLocked(false)
        .academicYear(year)
        .build();

    when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> periodService.deletePeriod(periodId)
    );

    assertEquals("ACADEMIC_PERIOD_CANNOT_BE_DELETED_NOT_PLANNED", ex.getMessage());
    verify(periodRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when period has sessions attached")
  void deletePeriod_hasSessions() {
    Long periodId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(10L)
        .status(YearStatus.PLANNED)
        .build();

    Period period = Period.builder()
        .id(periodId)
        .isLocked(false)
        .academicYear(year)
        .build();

    when(periodRepository.findById(periodId)).thenReturn(Optional.of(period));
    when(examSessionRepository.existsByPeriodId(periodId)).thenReturn(true);

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> periodService.deletePeriod(periodId)
    );

    assertEquals("ACADEMIC_PERIOD_CANNOT_BE_DELETED_HAS_SESSIONS", ex.getMessage());
    verify(periodRepository, never()).delete(any());
  }
}
