package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.dto.request.AcademicYearRequest;
import com.tiba.pts.modules.academicyear.dto.response.AcademicYearResponse;
import com.tiba.pts.modules.academicyear.dto.response.ActiveAcademicYearResponse;
import com.tiba.pts.modules.academicyear.mapper.AcademicYearMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearService {

  private final AcademicYearRepository academicYearRepository;
  private final AcademicYearMapper academicYearMapper;

  @Transactional
  public Long createAcademicYear(AcademicYearRequest request) {
    validateAcademicYear(request, null);

    AcademicYear academicYearToSave = academicYearMapper.toEntity(request);
    return academicYearRepository.save(academicYearToSave).getId();
  }

  @Transactional(readOnly = true)
  public AcademicYearResponse getById(Long id) {
    return academicYearRepository
        .findById(id)
        .map(academicYearMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public PageResponse<AcademicYearResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
    Page<AcademicYear> pageResult = academicYearRepository.findAll(pageable);
    return PageResponse.of(pageResult, academicYearMapper::toResponse);
  }

  @Transactional
  public Long updateAcademicYear(Long id, AcademicYearRequest request) {

    AcademicYear existingYear = getOrThrow(id);

    validateAcademicYear(request, id);

    academicYearMapper.updateEntityFromRequest(request, existingYear);

    return academicYearRepository.save(existingYear).getId();
  }

  @Transactional
  public void activate(Long id) {
    AcademicYear targetYear =
        academicYearRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Deactivate the currently active year if it's different from the target
    academicYearRepository
        .findByIsActiveTrue()
        .ifPresent(
            currentlyActive -> {
              if (!currentlyActive.getId().equals(targetYear.getId())) {
                currentlyActive.setIsActive(false);
                // Changes are automatically persisted by the transaction manager (dirty checking)
              }
            });

    // Activate the new year and update its status
    targetYear.setIsActive(true);
  }

  @Transactional
  public void deactivate(Long id) {
    AcademicYear year =
        academicYearRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Simply toggle the active flag to false
    year.setIsActive(false);
  }

  @Transactional
  public void changeStatus(Long id, YearStatus newStatus) {
    AcademicYear targetYear = getOrThrow(id);

    // Apply rules based on the new status
    switch (newStatus) {
      case IN_PROGRESS -> {
        // 1. Check if there is ALREADY another active year
        academicYearRepository
            .findByIsActiveTrue()
            .filter(currentlyActive -> !currentlyActive.getId().equals(targetYear.getId()))
            .ifPresent(
                currentlyActive -> {
                  // Throws an exception instead of silently deactivating the year
                  // Use the exception that matches your error handler (e.g., ValidationException)
                  throw new BusinessValidationException("ANOTHER_YEAR_IS_ALREADY_IN_PROGRESS");
                });

        // 2. Activate the new one
        targetYear.setStatus(YearStatus.IN_PROGRESS);
      }

      case PLANNED -> {
        targetYear.setIsActive(false);
        targetYear.setStatus(YearStatus.PLANNED);
      }

      case CLOSED -> {
        targetYear.setIsActive(false);
        targetYear.setStatus(YearStatus.CLOSED);
      }
    }
  }

  @Transactional(readOnly = true)
  public AcademicYearResponse getCurrentAcademicYear() {

    return academicYearRepository
        .findByIsActiveTrue()
        .map(academicYearMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("NO_ACTIVE_ACADEMIC_YEAR_FOUND"));
  }

  public ActiveAcademicYearResponse getCurrentActiveSession() {
    // 1. Retrieve the active academic year (isActive = true)
    AcademicYear activeYear =
        academicYearRepository
            .findByIsActiveTrue()
            .orElseThrow(() -> new RuntimeException("Aucune année académique active trouvée."));

    LocalDate today = LocalDate.now();

    // 2. Calculate remaining days
    long daysRemaining = ChronoUnit.DAYS.between(today, activeYear.getEndDate());
    // Optional: if the year has passed, cap at 0
    if (daysRemaining < 0) {
      daysRemaining = 0;
    }

    // 3. Determine the current period (e.g., Semester 1)
    String currentPeriodName = "N/A"; // Default value

    if (activeYear.getPeriods() != null && !activeYear.getPeriods().isEmpty()) {
      for (Period period : activeYear.getPeriods()) {
        // Check if today's date is between the start and end of the period
        if (!today.isBefore(period.getStartDate()) && !today.isAfter(period.getEndDate())) {
          currentPeriodName = period.getLabel();
          break;
        }
      }
    }

    // 4. Build and return the response
    return ActiveAcademicYearResponse.builder()
        .label(activeYear.getLabel())
        .startDate(activeYear.getStartDate())
        .endDate(activeYear.getEndDate())
        .currentPeriod(currentPeriodName)
        .daysRemaining(daysRemaining)
        .build();
  }

  @Transactional(readOnly = true)
  public List<AcademicYearResponse> getActiveOrPlannedYears() {
    // Pass YearStatus.PLANNED as a parameter for the "OrStatus" condition
    return academicYearRepository
        .findTop2ByIsActiveTrueOrStatusOrderByStartDateAsc(YearStatus.PLANNED)
        .stream()
        .map(academicYearMapper::toResponse)
        .toList();
  }

  // ==========================================
  //            PRIVATE Functions
  // ==========================================

  private AcademicYear getOrThrow(Long id) {
    return academicYearRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));
  }

  private void validateAcademicYear(AcademicYearRequest request, Long id) {
    boolean exists =
        (id == null)
            ? academicYearRepository.existsByLabelIgnoreCase(request.label())
            : academicYearRepository.existsByLabelIgnoreCaseAndIdNot(request.label(), id);

    if (exists) {
      throw new EntityAlreadyExistsException("LABEL_ALREADY_EXISTS");
    }
  }
}
