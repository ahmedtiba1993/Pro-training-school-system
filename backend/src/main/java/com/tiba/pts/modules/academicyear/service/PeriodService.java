package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.dto.request.PeriodRequest;
import com.tiba.pts.modules.academicyear.dto.response.PeriodResponse;
import com.tiba.pts.modules.academicyear.mapper.PeriodMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodService {

  private final PeriodRepository periodRepository;
  private final AcademicYearRepository academicYearRepository;
  private final PeriodMapper periodMapper;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long createPeriod(PeriodRequest request) {

    // Retrieve the Parent Year
    AcademicYear academicYear =
        academicYearRepository
            .findById(request.academicYearId())
            .orElseThrow(() -> new EntityNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Check the Academic Year (The Parent)
    YearStatus yearStatus = academicYear.getStatus();
    if (yearStatus == YearStatus.CLOSING || yearStatus == YearStatus.COMPLETED) {
      throw new BusinessValidationException("CREATION_FORBIDDEN_YEAR_IS_CLOSING_OR_CLOSED");
    }

    // Inclusion Rule (The House Walls)
    LocalDate pStart = request.startDate();
    LocalDate pEnd = request.endDate();
    LocalDate yStart = academicYear.getStartDate();
    LocalDate yEnd = academicYear.getEndDate();

    if (pStart.isBefore(yStart) || pEnd.isAfter(yEnd)) {
      throw new BusinessValidationException("PERIOD_DATES_MUST_BE_WITHIN_ACADEMIC_YEAR_DATES");
    }

    // Label uniqueness within the year (Optional, but good for data integrity)
    if (periodRepository.existsByAcademicYearIdAndLabelIgnoreCase(
        academicYear.getId(), request.label())) {
      throw new EntityAlreadyExistsException("PERIOD_LABEL_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Anti-Overlap Rule (Sibling Periods)
    if (periodRepository.existsOverlappingDates(academicYear.getId(), pStart, pEnd, null)) {
      throw new EntityAlreadyExistsException("PERIOD_DATES_OVERLAP_EXISTING_PERIOD");
    }

    // Mapping (Entity creation)
    Period period = periodMapper.toEntity(request);
    period.setAcademicYear(academicYear);
    period.setIsLocked(false);

    // Chronological Sort Calculation (The smart algorithm)
    // Retrieve existing periods
    List<Period> existingPeriods =
        periodRepository.findByAcademicYearIdOrderByStartDateAsc(academicYear.getId());

    // Calculate the position of the NEW period in the chronological flow
    int calculatedOrderIndex = calculateOrderIndex(existingPeriods, pStart);
    period.setOrderIndex(calculatedOrderIndex);

    // Save the new period
    Period savedPeriod = periodRepository.save(period);

    shiftOrderIndexForSubsequentPeriods(existingPeriods, calculatedOrderIndex);

    return savedPeriod.getId();
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

    // Retrieve the existing period
    Period existingPeriod =
        periodRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PERIOD_NOT_FOUND"));

    // The infamous Lock check (Fail-fast)
    if (Boolean.TRUE.equals(existingPeriod.getIsLocked())) {
      throw new BusinessValidationException("CANNOT_UPDATE_LOCKED_PERIOD");
    }

    // Check the Parent Year (Moving to another year is forbidden)
    AcademicYear academicYear = existingPeriod.getAcademicYear();
    if (!academicYear.getId().equals(request.academicYearId())) {
      throw new BusinessValidationException("CANNOT_MOVE_PERIOD_TO_ANOTHER_ACADEMIC_YEAR");
    }

    YearStatus yearStatus = academicYear.getStatus();
    if (yearStatus == YearStatus.CLOSING || yearStatus == YearStatus.COMPLETED) {
      throw new BusinessValidationException("UPDATE_FORBIDDEN_YEAR_IS_CLOSING_OR_COMPLETED");
    }

    // Inclusion Rule (Parent year boundaries)
    LocalDate pStart = request.startDate();
    LocalDate pEnd = request.endDate();
    if (pStart.isBefore(academicYear.getStartDate()) || pEnd.isAfter(academicYear.getEndDate())) {
      throw new BusinessValidationException("PERIOD_DATES_MUST_BE_WITHIN_ACADEMIC_YEAR_DATES");
    }

    // Label uniqueness (excluding the current period)
    boolean labelExists =
        periodRepository.existsByLabelIgnoreCaseAndAcademicYearIdAndIdNot(
            request.label(), academicYear.getId(), id);
    if (labelExists) {
      throw new EntityAlreadyExistsException("PERIOD_LABEL_ALREADY_EXISTS_IN_THIS_YEAR");
    }

    // Anti-Overlap (excluding the current period)
    if (periodRepository.existsOverlappingDates(academicYear.getId(), pStart, pEnd, id)) {
      throw new EntityAlreadyExistsException("PERIOD_DATES_OVERLAP_EXISTING_PERIOD");
    }

    // Should we recalculate the order? (If the start date changes)
    boolean startDateChanged = !existingPeriod.getStartDate().equals(pStart);

    // Update via MapStruct
    periodMapper.updatePeriodFromRequest(request, existingPeriod);

    Period updatedPeriod = periodRepository.save(existingPeriod);

    // Smart recalculation of the order if the date has changed
    if (startDateChanged) {
      recalculateAllOrderIndexes(academicYear.getId());
    }

    return periodMapper.toResponse(updatedPeriod);
  }

  /** Toggles the lock state (isLocked) of a period. */
  @Transactional
  public void togglePeriodLock(Long id) {
    // 1. Retrieve the period
    Period period =
        periodRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PERIOD_NOT_FOUND"));

    boolean isCurrentlyLocked = Boolean.TRUE.equals(period.getIsLocked());

    if (!isCurrentlyLocked) {
      if (period.getEndDate().isAfter(LocalDate.now())) {
        throw new BusinessValidationException("CANNOT_LOCK_PERIOD_BEFORE_ITS_END_DATE");
      }

    } else {

      // Rule: Check the Parent Year status
      YearStatus parentStatus = period.getAcademicYear().getStatus();

      if (parentStatus == YearStatus.COMPLETED) {
        // ABSOLUTE BLOCK
        throw new BusinessValidationException("CANNOT_UNLOCK_PERIOD_OF_COMPLETED_YEAR");
      }
    }

    // Toggle: Reverse the current value
    period.setIsLocked(!isCurrentlyLocked);

    // Save
    periodRepository.save(period);
  }

  @Transactional(readOnly = true)
  public List<PeriodResponse> getPeriodsByPromotion(Long promotionId) {

    // Retrieve and verify the existence of the Promotion
    Promotion promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    // Safeguard: Only accredited promotions have an academic year and periods
    if (!(promotion instanceof AccreditedPromotion accreditedPromotion)) {
      throw new BusinessValidationException("PROMOTION_MUST_BE_ACCREDITED_TO_HAVE_PERIODS");
    }

    // Extract the academic year ID
    Long academicYearId = accreditedPromotion.getAcademicYear().getId();

    // Retrieve sorted periods and map to response DTO
    return periodRepository.findByAcademicYearIdOrderByStartDateAsc(academicYearId).stream()
        .map(periodMapper::toResponse)
        .toList();
  }

  /** Algorithm to determine where the new period should be inserted chronologically */
  private int calculateOrderIndex(List<Period> sortedExistingPeriods, LocalDate newStartDate) {
    int index = 1; // Start at 1
    for (Period existingPeriod : sortedExistingPeriods) {
      // If the new period's date is BEFORE an existing period's date,
      // it means it should take its place.
      if (newStartDate.isBefore(existingPeriod.getStartDate())) {
        return index;
      }
      index++;
    }
    // If there is nothing "after", it goes last
    return index;
  }

  /** Shifts all orderIndexes of periods that come *after* the newly inserted period. */
  private void shiftOrderIndexForSubsequentPeriods(
      List<Period> sortedExistingPeriods, int startingIndex) {
    boolean hasChanged = false;
    int currentIndexToAssign = startingIndex + 1; // Start right after the new period's place

    for (int i = startingIndex - 1; i < sortedExistingPeriods.size(); i++) {
      Period pToUpdate = sortedExistingPeriods.get(i);
      pToUpdate.setOrderIndex(currentIndexToAssign);
      currentIndexToAssign++;
      hasChanged = true;
    }

    if (hasChanged) {
      periodRepository.saveAll(sortedExistingPeriods);
    }
  }

  /** A very robust method to recalculate all orderIndexes of a year instead of doing */
  private void recalculateAllOrderIndexes(Long academicYearId) {
    List<Period> allPeriods =
        periodRepository.findByAcademicYearIdOrderByStartDateAsc(academicYearId);
    int index = 1;
    for (Period period : allPeriods) {
      period.setOrderIndex(index++);
    }
    periodRepository.saveAll(allPeriods);
  }
}
