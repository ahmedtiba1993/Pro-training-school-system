package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.dto.request.AcademicYearRequest;
import com.tiba.pts.modules.academicyear.dto.response.AcademicYearResponse;
import com.tiba.pts.modules.academicyear.dto.response.ActiveAcademicYearResponse;
import com.tiba.pts.modules.academicyear.mapper.AcademicYearMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.HolidayRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.trainingsession.service.AccreditedPromotionService;
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
  private final PeriodRepository periodRepository;
  private final ExamSessionRepository examSessionRepository;
  private final HolidayRepository holidayRepository;
  private final AccreditedPromotionService accreditedPromotionService;

  @Transactional
  public Long createAcademicYear(AcademicYearRequest request) {
    validateAcademicYear(request, null);
    AcademicYear academicYear = academicYearMapper.toEntity(request);
    academicYear.setIsLocked(false);
    academicYear.setIsDefault(false);
    academicYear.setStatus(YearStatus.PLANNED);
    return academicYearRepository.save(academicYear).getId();
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
    YearStatus status = existingYear.getStatus();

    if (status != YearStatus.PLANNED
        && status != YearStatus.ENROLLMENT
        && status != YearStatus.IN_PROGRESS) {
      throw new BusinessValidationException("UPDATE_FORBIDDEN_FOR_CURRENT_STATUS");
    }

    if (status == YearStatus.IN_PROGRESS) {
      if (!existingYear.getLabel().equals(request.label())) {
        throw new BusinessValidationException("LABEL_NOT_MODIFIABLE_WHEN_IN_PROGRESS");
      }
      if (!existingYear.getStartDate().equals(request.startDate())) {
        throw new BusinessValidationException("START_DATE_NOT_MODIFIABLE_WHEN_IN_PROGRESS");
      }
    }

    // 🚨 New method: Protection against destructive shrinkage
    validateShrinkageProtection(id, request.startDate(), request.endDate());

    // Classic validation (Uniqueness & Overlapping)
    validateAcademicYear(request, id);

    // Mapping and Saving
    academicYearMapper.updateEntityFromRequest(request, existingYear);

    return academicYearRepository.save(existingYear).getId();
  }

  @Transactional(readOnly = true)
  public AcademicYearResponse getById(Long id) {
    return academicYearRepository
        .findById(id)
        .map(academicYearMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND"));
  }

  @Transactional
  public void setDefaultAcademicYear(Long id) {
    // Retrieve the new target year
    AcademicYear targetYear = getOrThrow(id);

    // Check if TargetYear is already the default
    if (Boolean.TRUE.equals(targetYear.getIsDefault())) {
      return;
    }

    // Check targetYear status
    YearStatus status = targetYear.getStatus();
    if (status == YearStatus.COMPLETED || status == YearStatus.PLANNED) {
      throw new BusinessValidationException("CANNOT_SET_DEFAULT_FOR_CLOSED_OR_PLANNED_YEAR");
    }

    // Look for current default year and revoke it
    academicYearRepository
        .findByIsDefaultTrue()
        .ifPresent(
            oldYear -> {
              oldYear.setIsDefault(false);
              academicYearRepository.save(oldYear);
            });

    // Apply the new status
    targetYear.setIsDefault(true);
    academicYearRepository.save(targetYear);
  }

  @Transactional
  public void changeStatus(Long id, YearStatus newStatus) {
    AcademicYear targetYear = getOrThrow(id);

    // do nothing if the status is already correct
    if (targetYear.getStatus() == newStatus) {
      academicYearMapper.toResponse(targetYear);
      return;
    }

    // Routing to specific transition logic
    switch (newStatus) {
      case ENROLLMENT -> processEnrollmentTransition(targetYear);
      case IN_PROGRESS -> processInProgressTransition(targetYear);
      case CLOSING -> processClosingTransition(targetYear);
      case COMPLETED -> processCompletedTransition(targetYear);
      default -> throw new RuntimeException("INVALID_STATUS_TRANSITION");
    }
    academicYearRepository.save(targetYear);

    accreditedPromotionService.syncStatusWithAcademicYear(id, newStatus);
  }

  @Transactional
  public void toggleLock(Long id) {
    AcademicYear targetYear = getOrThrow(id);

    // TOTAL PROHIBITION if the year is closed/completed
    if (targetYear.getStatus() == YearStatus.COMPLETED) {
      throw new BusinessValidationException("CANNOT_MODIFY_LOCK_ON_COMPLETED_YEAR");
    }

    // Toggle: Reverse current boolean value
    targetYear.setIsLocked(!targetYear.getIsLocked());

    // Save (always explicit as a best practice)
    academicYearRepository.save(targetYear);
  }

  /** Returns the list of all "open" academic years (Excludes CLOSING and COMPLETED). */
  @Transactional(readOnly = true)
  public List<AcademicYearResponse> getOpenAcademicYears() {

    List<YearStatus> excludedStatuses = List.of(YearStatus.CLOSING, YearStatus.COMPLETED);

    return academicYearRepository.findByStatusNotInOrderByStartDateDesc(excludedStatuses).stream()
        .map(academicYearMapper::toResponse)
        .toList();
  }

  /** Returns the currently ongoing academic year (IN_PROGRESS) */
  @Transactional(readOnly = true)
  public ActiveAcademicYearResponse getCurrentAcademicYear() {

    AcademicYear year =
        academicYearRepository
            .findByStatus(YearStatus.IN_PROGRESS)
            .orElseThrow(() -> new ResourceNotFoundException("NO_YEAR_CURRENTLY_IN_PROGRESS"));

    LocalDate today = LocalDate.now();

    // Overall Year Statistics
    long remainingDaysInYear = Math.max(0, ChronoUnit.DAYS.between(today, year.getEndDate()));

    // Period Statistics (Course period)
    String currentPeriodLabel = "No active period";
    int periodProgress = 0;
    long remainingDaysInCurrentPeriod = 0;

    // Find the term/semester that is active today
    var currentPeriodOpt = periodRepository.findCurrentPeriodByYearId(year.getId(), today);

    if (currentPeriodOpt.isPresent()) {
      Period currentPeriod = currentPeriodOpt.get();
      currentPeriodLabel = currentPeriod.getLabel();

      // Days before the end of courses (End of the term)
      remainingDaysInCurrentPeriod = ChronoUnit.DAYS.between(today, currentPeriod.getEndDate());
      remainingDaysInCurrentPeriod = Math.max(0, remainingDaysInCurrentPeriod);

      // Calculate progress percentage
      long totalDaysInPeriod =
          ChronoUnit.DAYS.between(currentPeriod.getStartDate(), currentPeriod.getEndDate());
      long passedDaysInPeriod = ChronoUnit.DAYS.between(currentPeriod.getStartDate(), today);

      if (totalDaysInPeriod > 0) {
        int calculatedProgress = (int) ((passedDaysInPeriod * 100.0) / totalDaysInPeriod);
        // Safety: Clamp percentage between 0 and 100
        periodProgress = Math.min(100, Math.max(0, calculatedProgress));
      }
    }

    // Build the response
    return academicYearMapper.toActiveResponse(
        year,
        remainingDaysInYear,
        currentPeriodLabel,
        periodProgress,
        remainingDaysInCurrentPeriod);
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
    // Validation of label uniqueness
    boolean labelExists =
        (id == null)
            ? academicYearRepository.existsByLabelIgnoreCase(request.label())
            : academicYearRepository.existsByLabelIgnoreCaseAndIdNot(request.label().trim(), id);

    if (labelExists) {
      throw new EntityAlreadyExistsException("ACADEMIC_YEAR_LABEL_ALREADY_EXISTS");
    }

    // Anti-overlapping validation (excludes the current ID if provided)
    boolean datesOverlap =
        academicYearRepository.existsOverlappingDates(request.startDate(), request.endDate(), id);

    if (datesOverlap) {
      throw new EntityAlreadyExistsException("ACADEMIC_YEAR_DATES_OVERLAP");
    }
  }

  private void processEnrollmentTransition(AcademicYear existingYear) {
    // Target year must be in PLANNED status
    if (existingYear.getStatus() != YearStatus.PLANNED) {
      throw new BusinessValidationException("STATUS_MUST_BE_PLANNED_TO_START_ENROLLMENT");
    }

    // No other year should currently be in ENROLLMENT
    if (academicYearRepository.existsByStatus(YearStatus.ENROLLMENT)) {
      throw new BusinessValidationException("ANOTHER_YEAR_IS_ALREADY_IN_ENROLLMENT");
    }

    // Apply new rules
    existingYear.setStatus(YearStatus.ENROLLMENT);
    existingYear.setIsLocked(false);

    // Save
    academicYearRepository.save(existingYear);
  }

  private void processInProgressTransition(AcademicYear targetYear) {
    // Current status MUST be ENROLLMENT
    if (targetYear.getStatus() != YearStatus.ENROLLMENT) {
      throw new BusinessValidationException("STATUS_MUST_BE_ENROLLMENT_TO_START_PROGRESS");
    }

    // Uniqueness of IN_PROGRESS status
    if (academicYearRepository.existsByStatus(YearStatus.IN_PROGRESS)) {
      throw new BusinessValidationException("ANOTHER_YEAR_IS_ALREADY_IN_PROGRESS");
    }

    // Verify start date (startDate <= today)
    if (LocalDate.now().isBefore(targetYear.getStartDate())) {
      throw new BusinessValidationException("CANNOT_START_YEAR_BEFORE_START_DATE");
    }

    // Apply new status
    targetYear.setStatus(YearStatus.IN_PROGRESS);
    targetYear.setIsLocked(false);
    academicYearRepository.save(targetYear);
    setDefaultAcademicYear(targetYear.getId());
  }

  private void processClosingTransition(AcademicYear targetYear) {
    // Current status MUST be IN_PROGRESS
    if (targetYear.getStatus() != YearStatus.IN_PROGRESS) {
      throw new BusinessValidationException("STATUS_MUST_BE_IN_PROGRESS_TO_START_CLOSING");
    }

    // Today's date must be strictly after the end date
    if (!LocalDate.now().isAfter(targetYear.getEndDate())) {
      throw new BusinessValidationException("CANNOT_CLOSE_YEAR_BEFORE_END_DATE");
    }

    // Apply new status and system LOCK
    targetYear.setStatus(YearStatus.CLOSING);
    targetYear.setIsLocked(true);

    // Save
    academicYearRepository.save(targetYear);
  }

  private void processCompletedTransition(AcademicYear targetYear) {
    // The year MUST be in CLOSING phase to be definitively completed
    if (targetYear.getStatus() != YearStatus.CLOSING) {
      throw new BusinessValidationException("STATUS_MUST_BE_CLOSING_TO_BE_CLOSED");
    }

    targetYear.setStatus(YearStatus.COMPLETED);
    targetYear.setIsLocked(true);

    // Save
    academicYearRepository.save(targetYear);
  }

  /**
   * Prevents shrinking the dates of an academic year if child entities (Periods, Exams, Holidays)
   * would fall outside the new boundaries.
   */
  private void validateShrinkageProtection(Long yearId, LocalDate newStart, LocalDate newEnd) {

    // The Period Barrier (Terms/Semesters)
    periodRepository
        .findMinStartDateByAcademicYearId(yearId)
        .ifPresent(
            minStart -> {
              if (newStart.isAfter(minStart)) {
                throw new BusinessValidationException("CANNOT_SHRINK_YEAR_BEFORE_FIRST_PERIOD");
              }
            });

    periodRepository
        .findMaxEndDateByAcademicYearId(yearId)
        .ifPresent(
            maxEnd -> {
              if (newEnd.isBefore(maxEnd)) {
                throw new BusinessValidationException("CANNOT_SHRINK_YEAR_AFTER_LAST_PERIOD");
              }
            });

    // The Holiday Barrier
    holidayRepository
        .findMinStartDateByAcademicYearId(yearId)
        .ifPresent(
            minStart -> {
              if (newStart.isAfter(minStart)) {
                throw new BusinessValidationException("CANNOT_SHRINK_YEAR_BEFORE_FIRST_HOLIDAY");
              }
            });

    holidayRepository
        .findMaxEndDateByAcademicYearId(yearId)
        .ifPresent(
            maxEnd -> {
              if (newEnd.isBefore(maxEnd)) {
                throw new BusinessValidationException("CANNOT_SHRINK_YEAR_AFTER_LAST_HOLIDAY");
              }
            });

    // The Retake Exams Barrier (RETAKE)
    examSessionRepository
        .findMaxEndDateByAcademicYearIdAndSessionType(yearId, SessionType.RETAKE)
        .ifPresent(
            maxRetakeEnd -> {
              if (newEnd.isBefore(maxRetakeEnd)) {
                throw new BusinessValidationException("CANNOT_SHRINK_YEAR_BEFORE_RETAKE_EXAMS");
              }
            });
  }
}
