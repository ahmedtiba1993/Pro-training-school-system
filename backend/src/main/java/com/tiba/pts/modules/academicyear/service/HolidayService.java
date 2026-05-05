package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.dto.request.HolidayRequest;
import com.tiba.pts.modules.academicyear.dto.response.HolidayResponse;
import com.tiba.pts.modules.academicyear.mapper.HolidayMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayService {

  private final HolidayRepository holidayRepository;
  private final AcademicYearRepository academicYearRepository;
  private final HolidayMapper holidayMapper;

  @Transactional
  public Long createHoliday(HolidayRequest request) {

    // Retrieve the Academic Year (Required for rules 4 and 5)
    AcademicYear academicYear =
        academicYearRepository
            .findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Check the Parent Year status
    YearStatus yearStatus = academicYear.getStatus();
    if (yearStatus == YearStatus.CLOSING || yearStatus == YearStatus.COMPLETED) {
      throw new BusinessValidationException("CREATION_FORBIDDEN_YEAR_IS_CLOSING_OR_COMPLETED");
    }

    // Prepare dates (If endDate is accidentally null, default it to startDate)
    LocalDate hStart = request.startDate();
    LocalDate hEnd = request.endDate() != null ? request.endDate() : hStart;

    // Inclusion Rule (Holiday dates must be WITHIN the academic year)
    if (hStart.isBefore(academicYear.getStartDate()) || hEnd.isAfter(academicYear.getEndDate())) {
      throw new BusinessValidationException("HOLIDAY_DATES_MUST_BE_WITHIN_ACADEMIC_YEAR_DATES");
    }

    // Anti-Overlap (Holidays must not overlap)
    if (holidayRepository.existsOverlappingDates(academicYear.getId(), hStart, hEnd, null)) {
      throw new EntityAlreadyExistsException("HOLIDAY_DATES_OVERLAP_EXISTING_HOLIDAY");
    }

    // Label uniqueness (The rule you already had)
    if (holidayRepository.existsByLabelIgnoreCaseAndAcademicYearId(
        request.label(), academicYear.getId())) {
      throw new EntityAlreadyExistsException("HOLIDAY_LABEL_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Mapping and Save
    Holiday holidayToSave = holidayMapper.toEntity(request);
    holidayToSave.setAcademicYear(academicYear);

    return holidayRepository.save(holidayToSave).getId();
  }

  @Transactional(readOnly = true)
  public List<HolidayResponse> getAllHolidays() {
    return holidayRepository.findAll(Sort.by(Sort.Direction.ASC, "startDate")).stream()
        .map(holidayMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<HolidayResponse> getHolidaysByAcademicYear(Long academicYearId) {

    if (!academicYearRepository.existsById(academicYearId)) {
      throw new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND");
    }

    return holidayRepository.findByAcademicYearIdOrderByStartDateAsc(academicYearId).stream()
        .map(holidayMapper::toResponse)
        .toList();
  }

  @Transactional
  public HolidayResponse updateHoliday(Long id, HolidayRequest request) {

    // Retrieve the existing holiday
    Holiday existingHoliday =
        holidayRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("HOLIDAY_NOT_FOUND"));

    AcademicYear academicYear = existingHoliday.getAcademicYear();

    // Anti-Move Safety
    if (!academicYear.getId().equals(request.academicYearId())) {
      throw new BusinessValidationException("CANNOT_MOVE_HOLIDAY_TO_ANOTHER_ACADEMIC_YEAR");
    }

    // Check the Parent Year status
    YearStatus yearStatus = academicYear.getStatus();
    if (yearStatus == YearStatus.CLOSING || yearStatus == YearStatus.COMPLETED) {
      throw new BusinessValidationException("UPDATE_FORBIDDEN_YEAR_IS_CLOSING_OR_COMPLETED");
    }

    // Prepare dates
    LocalDate hStart = request.startDate();
    LocalDate hEnd = request.endDate() != null ? request.endDate() : hStart;

    // Inclusion Rule
    if (hStart.isBefore(academicYear.getStartDate()) || hEnd.isAfter(academicYear.getEndDate())) {
      throw new BusinessValidationException("HOLIDAY_DATES_MUST_BE_WITHIN_ACADEMIC_YEAR_DATES");
    }

    // Label uniqueness (Excludes the current ID)
    if (holidayRepository.existsByLabelIgnoreCaseAndAcademicYearIdAndIdNot(
        request.label(), academicYear.getId(), id)) {
      throw new EntityAlreadyExistsException("HOLIDAY_LABEL_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Anti-Overlap (Excludes the current ID)
    if (holidayRepository.existsOverlappingDates(academicYear.getId(), hStart, hEnd, id)) {
      throw new EntityAlreadyExistsException("HOLIDAY_DATES_OVERLAP_EXISTING_HOLIDAY");
    }

    // Apply modifications
    holidayMapper.updateEntityFromRequest(request, existingHoliday);

    // Safety: Force the endDate manually in case it was null in the request
    existingHoliday.setEndDate(hEnd);

    // Save
    Holiday savedHoliday = holidayRepository.save(existingHoliday);

    // We assume you have a toResponse method in your mapper
    return holidayMapper.toResponse(savedHoliday);
  }
}
