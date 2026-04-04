package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import com.tiba.pts.modules.academicyear.dto.request.HolidayRequest;
import com.tiba.pts.modules.academicyear.dto.response.HolidayResponse;
import com.tiba.pts.modules.academicyear.mapper.HolidayMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayService {

  private final HolidayRepository holidayRepository;
  private final AcademicYearRepository academicYearRepository;
  private final HolidayMapper holidayMapper;

  @Transactional
  public Long createHoliday(HolidayRequest request) {

    // Verify the existence of the parent academic year
    if (!academicYearRepository.existsById(request.academicYearId())) {
      throw new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND");
    }

    // Uniqueness check (for the same year)
    if (holidayRepository.existsByTitleIgnoreCaseAndAcademicYearId(
        request.title(), request.academicYearId())) {
      throw new EntityAlreadyExistsException("HOLIDAY_TITLE_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    Holiday holidayToSave = holidayMapper.toEntity(request);
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

    // if the year does not exist, throw 404
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

    // If the academic year has changed, verify that it exists
    if (!existingHoliday.getAcademicYear().getId().equals(request.academicYearId())) {
      if (!academicYearRepository.existsById(request.academicYearId())) {
        throw new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND");
      }
    }

    // Uniqueness check (for the same year, excluding the current holiday ID)
    if (holidayRepository.existsByTitleIgnoreCaseAndAcademicYearIdAndIdNot(
        request.title(), request.academicYearId(), id)) {
      throw new EntityAlreadyExistsException("HOLIDAY_TITLE_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Update the entity with new data via MapStruct
    holidayMapper.updateEntityFromRequest(request, existingHoliday);

    // Save and return the response
    Holiday updatedHoliday = holidayRepository.save(existingHoliday);
    return holidayMapper.toResponse(updatedHoliday);
  }
}
