package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.mapper.HolidayMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.HolidayRepository;
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
class HolidayServiceDeleteTest {

  @Mock
  private HolidayRepository holidayRepository;

  @Mock
  private AcademicYearRepository academicYearRepository;

  @Mock
  private HolidayMapper holidayMapper;

  @InjectMocks
  private HolidayService holidayService;

  @Test
  @DisplayName("Should successfully delete holiday when parent year is PLANNED")
  void deleteHoliday_success() {
    Long holidayId = 1L;
    AcademicYear year = AcademicYear.builder().status(YearStatus.PLANNED).build();
    Holiday holiday = Holiday.builder()
        .id(holidayId)
        .academicYear(year)
        .build();

    when(holidayRepository.findById(holidayId)).thenReturn(Optional.of(holiday));

    holidayService.deleteHoliday(holidayId);

    verify(holidayRepository).delete(holiday);
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when holiday not found")
  void deleteHoliday_notFound() {
    Long holidayId = 99L;
    when(holidayRepository.findById(holidayId)).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> holidayService.deleteHoliday(holidayId)
    );

    assertEquals("ACADEMIC_HOLIDAY_NOT_FOUND", ex.getMessage());
    verify(holidayRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should throw BusinessValidationException when parent year is not PLANNED")
  void deleteHoliday_yearNotPlanned() {
    Long holidayId = 1L;
    AcademicYear year = AcademicYear.builder().status(YearStatus.IN_PROGRESS).build();
    Holiday holiday = Holiday.builder()
        .id(holidayId)
        .academicYear(year)
        .build();

    when(holidayRepository.findById(holidayId)).thenReturn(Optional.of(holiday));

    BusinessValidationException ex = assertThrows(
        BusinessValidationException.class,
        () -> holidayService.deleteHoliday(holidayId)
    );

    assertEquals("ACADEMIC_HOLIDAY_CANNOT_BE_DELETED_NOT_PLANNED", ex.getMessage());
    verify(holidayRepository, never()).delete(any());
  }
}
