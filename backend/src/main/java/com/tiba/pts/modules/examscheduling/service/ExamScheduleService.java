package com.tiba.pts.modules.examscheduling.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamRoomAllocation;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamSchedule;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimeSlot;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.examscheduling.domain.enums.ExamTimetableStatus;
import com.tiba.pts.modules.examscheduling.dto.request.ExamScheduleRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamScheduleResponse;
import com.tiba.pts.modules.examscheduling.mapper.ExamScheduleMapper;
import com.tiba.pts.modules.examscheduling.repository.ExamScheduleRepository;
import com.tiba.pts.modules.examscheduling.repository.ExamTimeSlotRepository;
import com.tiba.pts.modules.examscheduling.repository.ExamTimetableRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamScheduleService {

  private final ExamScheduleRepository examScheduleRepository;
  private final ExamTimetableRepository examTimetableRepository;
  private final ExamScheduleMapper examScheduleMapper;
  private final AssessmentRepository assessmentRepository;
  private final ExamTimeSlotRepository examTimeSlotRepository;

  @Transactional
  public Long createSchedule(Long timetableId, ExamScheduleRequest request) {
    ExamTimetable timetable =
        examTimetableRepository
            .findById(timetableId)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIMETABLE_NOT_FOUND"));

    // Validation: Cannot add schedule if timetable is published
    if (timetable.getStatus() == ExamTimetableStatus.PUBLISHED) {
      throw new BusinessValidationException("TIMETABLE_ALREADY_PUBLISHED");
    }

    Assessment assessment =
        assessmentRepository
            .findById(request.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    ExamTimeSlot timeSlot =
        examTimeSlotRepository
            .findById(request.examTimeSlotId())
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIME_SLOT_NOT_FOUND"));

    // Validation: Check if the assessment is an official exam (FINAL_EXAM or RETAKE)
    AssessmentType examType = assessment.getAssessmentType();
    if (examType != AssessmentType.FINAL_EXAM && examType != AssessmentType.RETAKE) {
      throw new BusinessValidationException("NOT_AN_OFFICIAL_EXAM");
    }

    // Validation 2: The class cannot have two exams at the same time
    if (examScheduleRepository.existsByExamTimetableClassGroupIdAndExamDateAndExamTimeSlotId(
        timetable.getClassGroup().getId(), request.examDate(), request.examTimeSlotId())) {
      throw new BusinessValidationException("CLASS_ALREADY_SCHEDULED_AT_THIS_TIME");
    }

    // Validation 3: Same assessment cannot be added twice in the same timetable
    if (examScheduleRepository.existsByExamTimetableIdAndAssessmentId(
        timetableId, request.assessmentId())) {
      throw new BusinessValidationException("ASSESSMENT_ALREADY_SCHEDULED_IN_THIS_TIMETABLE");
    }

    ExamSchedule examSchedule = examScheduleMapper.toEntity(request);
    examSchedule.setExamTimetable(timetable);
    examSchedule.setAssessment(assessment);
    examSchedule.setExamTimeSlot(timeSlot);

    return examScheduleRepository.save(examSchedule).getId();
  }

  @Transactional
  public Long updateSchedule(Long scheduleId, ExamScheduleRequest request) {
    ExamSchedule schedule =
        examScheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SCHEDULE_NOT_FOUND"));

    // Validation: Cannot modify schedule if timetable is published
    if (schedule.getExamTimetable().getStatus() == ExamTimetableStatus.PUBLISHED) {
      throw new BusinessValidationException("TIMETABLE_ALREADY_PUBLISHED");
    }

    ExamTimeSlot timeSlot =
        examTimeSlotRepository
            .findById(request.examTimeSlotId())
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIME_SLOT_NOT_FOUND"));

    Assessment assessment =
        assessmentRepository
            .findById(request.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    // Validation: Same class cannot do two exams at the same time (excluding current schedule)
    if (examScheduleRepository
        .existsByExamTimetableClassGroupIdAndExamDateAndExamTimeSlotIdAndIdNot(
            schedule.getExamTimetable().getClassGroup().getId(),
            request.examDate(),
            request.examTimeSlotId(),
            scheduleId)) {
      throw new BusinessValidationException("CLASS_ALREADY_SCHEDULED_AT_THIS_TIME");
    }

    // Validation: Check if the assessment is an official exam (FINAL_EXAM or RETAKE)
    AssessmentType examType = assessment.getAssessmentType();
    if (examType != AssessmentType.FINAL_EXAM && examType != AssessmentType.RETAKE) {
      throw new BusinessValidationException("NOT_AN_OFFICIAL_EXAM");
    }

    // Validation: Same assessment cannot be added twice in the same timetable
    if (!schedule.getAssessment().getId().equals(request.assessmentId())) {
      if (examScheduleRepository.existsByExamTimetableIdAndAssessmentId(
          schedule.getExamTimetable().getId(), request.assessmentId())) {
        throw new BusinessValidationException("ASSESSMENT_ALREADY_SCHEDULED_IN_THIS_TIMETABLE");
      }
      schedule.setAssessment(assessment);
    }

    schedule.setExamDate(request.examDate());
    schedule.setExamTimeSlot(timeSlot);

    return examScheduleRepository.save(schedule).getId();
  }

  @Transactional
  public void removeSchedule(Long scheduleId) {
    ExamSchedule schedule =
        examScheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SCHEDULE_NOT_FOUND"));

    // Validation: Cannot delete schedule if timetable is published
    if (schedule.getExamTimetable().getStatus() == ExamTimetableStatus.PUBLISHED) {
      throw new BusinessValidationException("TIMETABLE_ALREADY_PUBLISHED");
    }

    // Validation: Cannot delete if the exam date is in the past
    if (schedule.getExamDate().isBefore(java.time.LocalDate.now())) {
      throw new BusinessValidationException("CANNOT_DELETE_PAST_EXAM");
    }

    examScheduleRepository.delete(schedule);
  }

  @Transactional(readOnly = true)
  public java.util.List<ExamScheduleResponse> getSchedulesByTimetableId(Long timetableId) {
    if (!examTimetableRepository.existsById(timetableId)) {
      throw new ResourceNotFoundException("EXAM_TIMETABLE_NOT_FOUND");
    }
    return examScheduleRepository.findByExamTimetableId(timetableId).stream()
        .map(examScheduleMapper::toResponse)
        .toList();
  }
}
