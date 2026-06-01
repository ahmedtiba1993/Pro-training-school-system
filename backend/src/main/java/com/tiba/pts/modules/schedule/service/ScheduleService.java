package com.tiba.pts.modules.schedule.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import com.tiba.pts.modules.classmanagement.repository.ClassGroupRepository;
import com.tiba.pts.modules.schedule.domain.entity.Schedule;
import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import com.tiba.pts.modules.schedule.dto.request.ScheduleRequest;
import com.tiba.pts.modules.schedule.dto.request.ScheduleStatusRequest;
import com.tiba.pts.modules.schedule.dto.request.ScheduleUpdateRequest;
import com.tiba.pts.modules.schedule.dto.response.ScheduleResponse;
import com.tiba.pts.modules.schedule.mapper.ScheduleMapper;
import com.tiba.pts.modules.schedule.repository.ScheduleRepository;
import com.tiba.pts.modules.schedule.repository.TimetableSlotRepository;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

  private final ScheduleRepository scheduleRepository;
  private final ClassGroupRepository classGroupRepository;
  private final PeriodRepository periodRepository;
  private final ScheduleMapper scheduleMapper;
  private final TimetableSlotRepository timetableSlotRepository;

  @Transactional
  public Long createSchedule(ScheduleRequest request) {

    // Validate the existence and status of the class
    ClassGroup classGroup =
        classGroupRepository
            .findById(request.getClassGroupId())
            .orElseThrow(() -> new ResourceNotFoundException("CLASS_GROUP_NOT_FOUND"));

    if (classGroup.getStatus() == ClassStatus.ARCHIVED) {
      throw new BusinessValidationException("CLASS_GROUP_ALREADY_ARCHIVED");
    }

    // Identify the training type (SSOT read from the catalog via the promotion)
    TrainingType trainingType = classGroup.getPromotion().getTraining().getTrainingType();
    Period period = null;

    // --- CASE A: ACCREDITED TRAINING ---
    if (trainingType == TrainingType.ACCREDITED) {
      if (request.getPeriodId() == null) {
        throw new RuntimeException("PERIOD_ID_REQUIRED_FOR_ACCREDITED_TRAINING");
      }

      period =
          periodRepository
              .findById(request.getPeriodId())
              .orElseThrow(() -> new ResourceNotFoundException("PERIOD_NOT_FOUND"));

      // Period status: Forbidden if locked (isLocked = true)
      if (Boolean.TRUE.equals(period.getIsLocked())) {
        throw new BusinessValidationException("PERIOD_IS_LOCKED");
      }

      // Strict uniqueness of the pair [class_group_id + academic_period_id]
      if (scheduleRepository.existsByClassGroupIdAndPeriodId(
          request.getClassGroupId(), request.getPeriodId())) {
        throw new EntityAlreadyExistsException("SCHEDULE_ALREADY_EXISTS_FOR_CLASS_AND_PERIOD");
      }
    }

    // --- CASE B: ACCELERATED OR CONTINUOUS TRAINING ---
    else if (trainingType == TrainingType.ACCELERATED || trainingType == TrainingType.CONTINUOUS) {
      if (request.getPeriodId() != null) {
        throw new BusinessValidationException("PERIOD_ID_FORBIDDEN_FOR_NON_ACCREDITED_TRAINING");
      }

      // Strict global uniqueness for the ClassGroup where period is null
      if (scheduleRepository.existsByClassGroupIdAndPeriodIsNull(request.getClassGroupId())) {
        throw new EntityAlreadyExistsException("SCHEDULE_ALREADY_EXISTS_FOR_THIS_CONTINUOUS_CLASS");
      }
    }

    // Mapping, associations, and persistence
    Schedule schedule = scheduleMapper.toEntity(request);
    schedule.setClassGroup(classGroup);
    schedule.setPeriod(period);
    schedule.setStatus(ScheduleStatus.DRAFT);

    return scheduleRepository.save(schedule).getId();
  }

  @Transactional(readOnly = true)
  public List<ScheduleResponse> findAllByStatus(ScheduleStatus status) {
    return scheduleRepository.findAllByStatus(status).stream()
        .map(scheduleMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public ScheduleResponse updateSchedule(Long id, ScheduleUpdateRequest request) {

    // Retrieve the existing schedule
    Schedule schedule =
        scheduleRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SCHEDULE_NOT_FOUND"));

    // If the Schedule has ARCHIVED status, no modification is allowed
    if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
      throw new BusinessValidationException("CANNOT_UPDATE_ARCHIVED_SCHEDULE");
    }

    // Apply the update (Only the label is modified)
    // Followed rules: Class, Period, and Status remain strictly unchanged
    scheduleMapper.updateEntityFromRequest(request, schedule);

    // Thanks to Hibernate's Dirty Checking mechanism coupled with @Transactional,
    // the modified entity will be persisted automatically at the end of the method.
    return scheduleMapper.toResponse(schedule);
  }

  @Transactional
  public ScheduleResponse changeStatus(Long id, ScheduleStatusRequest request) {

    ScheduleStatus newStatus = request.getNewStatus();

    // Retrieve the schedule
    Schedule schedule =
        scheduleRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SCHEDULE_NOT_FOUND"));

    ScheduleStatus currentStatus = schedule.getStatus();

    // STATE MACHINE VALIDATION
    validateTransition(currentStatus, newStatus);

    // SPECIFIC BUSINESS RULES FOR DRAFT → ACTIVE
    if (currentStatus == ScheduleStatus.DRAFT && newStatus == ScheduleStatus.ACTIVE) {
      applyActivationRules(schedule);
    }

    // Apply status change
    schedule.setStatus(newStatus);

    return scheduleMapper.toResponse(schedule);
  }

  /** Validates that the requested transition is allowed by the state machine. */
  private void validateTransition(ScheduleStatus currentStatus, ScheduleStatus newStatus) {

    // No change → unnecessary
    if (currentStatus == newStatus) {
      throw new BusinessValidationException("SCHEDULE_STATUS_UNCHANGED");
    }

    // ARCHIVED → * : terminal state, no rollback possible
    if (currentStatus == ScheduleStatus.ARCHIVED) {
      throw new BusinessValidationException("CANNOT_CHANGE_ARCHIVED_SCHEDULE_STATUS");
    }

    // ACTIVE → DRAFT : forbidden (students can already see this schedule)
    if (currentStatus == ScheduleStatus.ACTIVE && newStatus == ScheduleStatus.DRAFT) {
      throw new BusinessValidationException("CANNOT_REVERT_ACTIVE_TO_DRAFT");
    }

    // Only valid transitions:
    //   DRAFT → ACTIVE
    //   DRAFT → ARCHIVED
    //   ACTIVE → ARCHIVED
    boolean validTransition =
        (currentStatus == ScheduleStatus.DRAFT
                && (newStatus == ScheduleStatus.ACTIVE || newStatus == ScheduleStatus.ARCHIVED))
            || (currentStatus == ScheduleStatus.ACTIVE && newStatus == ScheduleStatus.ARCHIVED);

    if (!validTransition) {
      throw new BusinessValidationException("INVALID_SCHEDULE_STATUS_TRANSITION");
    }
  }

  /** Specific business rules during activation (DRAFT → ACTIVE). */
  private void applyActivationRules(Schedule schedule) {

    // The schedule must contain at least one class slot
    if (!timetableSlotRepository.existsByScheduleId(schedule.getId())) {
      throw new BusinessValidationException("CANNOT_ACTIVATE_EMPTY_SCHEDULE");
    }

    ClassGroup classGroup = schedule.getClassGroup();

    // The class must not be archived
    if (classGroup.getStatus() == ClassStatus.ARCHIVED) {
      throw new BusinessValidationException("CANNOT_ACTIVATE_SCHEDULE_CLASS_ARCHIVED");
    }

    // The academic period (if present) must not be locked
    Period period = schedule.getPeriod();
    if (period != null && Boolean.TRUE.equals(period.getIsLocked())) {
      throw new BusinessValidationException("CANNOT_ACTIVATE_SCHEDULE_PERIOD_LOCKED");
    }

    // Uniqueness — Only one ACTIVE per ClassGroup (+Period if accredited)
    // Identify the training type
    TrainingType trainingType = classGroup.getPromotion().getTraining().getTrainingType();

    if (trainingType == TrainingType.ACCREDITED) {
      // Accredited training → check ClassGroup + Period uniqueness
      if (period != null
          && scheduleRepository.existsByClassGroupIdAndPeriodIdAndStatus(
              classGroup.getId(), period.getId(), ScheduleStatus.ACTIVE)) {
        throw new BusinessValidationException(
            "ACTIVE_SCHEDULE_ALREADY_EXISTS_FOR_CLASS_AND_PERIOD");
      }
    } else {
      // Continuous/accelerated training → check ClassGroup uniqueness (without period)
      if (scheduleRepository.existsByClassGroupIdAndPeriodIsNullAndStatus(
          classGroup.getId(), ScheduleStatus.ACTIVE)) {
        throw new BusinessValidationException("ACTIVE_SCHEDULE_ALREADY_EXISTS_FOR_CLASS");
      }
    }
  }
}
