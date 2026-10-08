package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.mapper.AcademicYearMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.HolidayRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.trainingsession.service.AccreditedPromotionService;
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
class AcademicYearServiceDeleteTest {

  @Mock
  private AcademicYearRepository academicYearRepository;

  @Mock
  private AcademicYearMapper academicYearMapper;

  @Mock
  private PeriodRepository periodRepository;

  @Mock
  private ExamSessionRepository examSessionRepository;

  @Mock
  private HolidayRepository holidayRepository;

  @Mock
  private AccreditedPromotionService accreditedPromotionService;

  @InjectMocks
  private AcademicYearService academicYearService;

  @Test
  @DisplayName("Should successfully delete academic year when status is PLANNED, not default, and no dependencies")
  void deleteAcademicYear_success() {
    Long yearId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(yearId)
        .status(YearStatus.PLANNED)
        .isDefault(false)
        .build();

    when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));
    when(periodRepository.existsByAcademicYearId(yearId)).thenReturn(false);
    when(accreditedPromotionService.hasPromotionsForAcademicYear(yearId)).thenReturn(false);

    academicYearService.deleteAcademicYear(yearId);

    verify(academicYearRepository).delete(year);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when academic year does not exist")
  void deleteAcademicYear_notFound() {
    Long yearId = 99L;
    when(academicYearRepository.findById(yearId)).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> academicYearService.deleteAcademicYear(yearId)
    );

    assertEquals("ACADEMIC_YEAR_NOT_FOUND", ex.getMessage());
    verify(academicYearRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when status is not PLANNED")
  void deleteAcademicYear_notPlanned() {
    Long yearId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(yearId)
        .status(YearStatus.IN_PROGRESS)
        .isDefault(false)
        .build();

    when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> academicYearService.deleteAcademicYear(yearId)
    );

    assertEquals("ACADEMIC_YEAR_CANNOT_BE_DELETED_NOT_PLANNED", ex.getMessage());
    verify(academicYearRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when year is default")
  void deleteAcademicYear_isDefault() {
    Long yearId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(yearId)
        .status(YearStatus.PLANNED)
        .isDefault(true)
        .build();

    when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> academicYearService.deleteAcademicYear(yearId)
    );

    assertEquals("ACADEMIC_YEAR_CANNOT_BE_DELETED_IS_DEFAULT", ex.getMessage());
    verify(academicYearRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when year has periods")
  void deleteAcademicYear_hasPeriods() {
    Long yearId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(yearId)
        .status(YearStatus.PLANNED)
        .isDefault(false)
        .build();

    when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));
    when(periodRepository.existsByAcademicYearId(yearId)).thenReturn(true);

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> academicYearService.deleteAcademicYear(yearId)
    );

    assertEquals("ACADEMIC_YEAR_CANNOT_BE_DELETED_HAS_PERIODS", ex.getMessage());
    verify(academicYearRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when year has accredited promotions")
  void deleteAcademicYear_hasPromotions() {
    Long yearId = 1L;
    AcademicYear year = AcademicYear.builder()
        .id(yearId)
        .status(YearStatus.PLANNED)
        .isDefault(false)
        .build();

    when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));
    when(periodRepository.existsByAcademicYearId(yearId)).thenReturn(false);
    when(accreditedPromotionService.hasPromotionsForAcademicYear(yearId)).thenReturn(true);

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> academicYearService.deleteAcademicYear(yearId)
    );

    assertEquals("ACADEMIC_YEAR_CANNOT_BE_DELETED_HAS_PROMOTIONS", ex.getMessage());
    verify(academicYearRepository, never()).delete(any());
  }
}
