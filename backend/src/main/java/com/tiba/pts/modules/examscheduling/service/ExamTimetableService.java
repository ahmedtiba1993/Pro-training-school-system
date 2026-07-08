package com.tiba.pts.modules.examscheduling.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.repository.ClassGroupRepository;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.examscheduling.domain.enums.ExamTimetableStatus;
import com.tiba.pts.modules.examscheduling.dto.request.ExamTimetableRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamTimetableResponse;
import com.tiba.pts.modules.examscheduling.mapper.ExamTimetableMapper;
import com.tiba.pts.modules.examscheduling.repository.ExamTimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamTimetableService {

  private final ExamTimetableRepository examTimetableRepository;
  private final ExamTimetableMapper examTimetableMapper;
  private final ClassGroupRepository classGroupRepository;
  private final PeriodRepository periodRepository;
  private final ExamSessionRepository examSessionRepository;

  @Transactional
  public Long createTimetable(ExamTimetableRequest request) {

    ClassGroup classGroup =
        classGroupRepository
            .findById(request.classGroupId())
            .orElseThrow(() -> new ResourceNotFoundException("CLASS_GROUP_NOT_FOUND"));

    Period period =
        periodRepository
            .findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("PERIOD_NOT_FOUND"));

    ExamSession examSession =
        examSessionRepository
            .findById(request.examSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SESSION_NOT_FOUND"));

    if (examTimetableRepository.existsByClassGroupIdAndExamSessionId(
        request.classGroupId(), request.examSessionId())) {
      throw new BusinessValidationException("TIMETABLE_ALREADY_EXISTS_FOR_CLASS_AND_SESSION");
    }

    ExamTimetable examTimetable = examTimetableMapper.toEntity(request);
    examTimetable.setClassGroup(classGroup);
    examTimetable.setPeriod(period);
    examTimetable.setExamSession(examSession);
    examTimetable.setStatus(ExamTimetableStatus.DRAFT);

    return examTimetableRepository.save(examTimetable).getId();
  }

  @Transactional
  public void publishTimetable(Long id) {
    ExamTimetable examTimetable =
        examTimetableRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIMETABLE_NOT_FOUND"));

    examTimetable.setStatus(ExamTimetableStatus.PUBLISHED);
    examTimetableRepository.save(examTimetable);
  }

  @Transactional(readOnly = true)
  public ExamTimetableResponse getTimetableById(Long id) {
    return examTimetableRepository
        .findById(id)
        .map(examTimetableMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIMETABLE_NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public java.util.List<ExamTimetableResponse> getAllTimetables() {
    return examTimetableRepository.findAll().stream()
        .map(examTimetableMapper::toResponse)
        .toList();
  }
}
