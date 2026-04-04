package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.dto.request.PeriodRequest;
import com.tiba.pts.modules.academicyear.dto.response.PeriodResponse;
import com.tiba.pts.modules.academicyear.mapper.PeriodMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodService {

  private final PeriodRepository periodRepository;
  private final AcademicYearRepository academicYearRepository;
  private final PeriodMapper periodMapper;

  @Transactional
  public Long createPeriod(PeriodRequest request) {

    // Verify if the academic year exists
    if (!academicYearRepository.existsById(request.academicYearId())) {
      throw new ResourceNotFoundException("ACADEMIC_YEAR_DOES_NOT_EXIST");
    }

    // Check uniqueness (the same label can only exist once within THE SAME academic year)
    if (periodRepository.existsByLabelIgnoreCaseAndAcademicYearId(
        request.label(), request.academicYearId())) {
      throw new EntityAlreadyExistsException("PERIOD_LABEL_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Mapping and Saving
    Period periodToSave = periodMapper.toEntity(request);
    return periodRepository.save(periodToSave).getId();
  }

  @Transactional(readOnly = true)
  public List<PeriodResponse> getPeriodsByAcademicYear(Long academicYearId) {

    // Verify if the year exists to return a clean 404 error
    if (!academicYearRepository.existsById(academicYearId)) {
      throw new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND");
    }

    return periodRepository.findByAcademicYearIdOrderByStartDateAsc(academicYearId).stream()
        .map(periodMapper::toResponse)
        .toList();
  }

  @Transactional
  public PeriodResponse updatePeriod(Long id, PeriodRequest request) {

    //  Retrieve the existing period
    Period existingPeriod =
        periodRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PERIOD_NOT_FOUND"));

    // Check label uniqueness (excluding the current period by its ID)
    boolean labelExists =
        periodRepository.existsByLabelIgnoreCaseAndAcademicYearIdAndIdNot(
            request.label(), request.academicYearId(), id);

    if (labelExists) {
      throw new EntityAlreadyExistsException("PERIOD_LABEL_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Update basic data via MapStruct
    periodMapper.updatePeriodFromRequest(request, existingPeriod);

    // Handle academic year (if it has been modified)
    if (!existingPeriod.getAcademicYear().getId().equals(request.academicYearId())) {
      if (!academicYearRepository.existsById(request.academicYearId())) {
        throw new ResourceNotFoundException("ACADEMIC_YEAR_DOES_NOT_EXIST");
      }
      // getReferenceById avoids an unnecessary SELECT query; it just creates a proxy with the ID
      existingPeriod.setAcademicYear(
          academicYearRepository.getReferenceById(request.academicYearId()));
    }

    // Save
    Period updatedPeriod = periodRepository.save(existingPeriod);
    return periodMapper.toResponse(updatedPeriod);
  }
}
