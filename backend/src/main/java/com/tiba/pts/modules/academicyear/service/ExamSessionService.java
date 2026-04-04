package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.enums.SessionStatus;
import com.tiba.pts.modules.academicyear.dto.request.ExamSessionRequest;
import com.tiba.pts.modules.academicyear.dto.response.ExamSessionResponse;
import com.tiba.pts.modules.academicyear.mapper.ExamSessionMapper;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
// repository
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamSessionService {

  private final ExamSessionRepository examSessionRepository;
  private final PeriodRepository periodRepository;
  private final ExamSessionMapper examSessionMapper;

  @Transactional
  public Long createExamSession(ExamSessionRequest request) {

    // Verify that the parent period exists
    if (!periodRepository.existsById(request.periodId())) {
      throw new ResourceNotFoundException("PERIOD_NOT_FOUND");
    }

    // Check label uniqueness for THIS period
    if (examSessionRepository.existsByLabelIgnoreCaseAndPeriodId(
        request.label(), request.periodId())) {
      throw new EntityAlreadyExistsException("EXAM_SESSION_LABEL_ALREADY_EXISTS_IN_THIS_PERIOD");
    }

    // Check session TYPE uniqueness (e.g., impossible to have two MAIN sessions in the same period)
    if (examSessionRepository.existsBySessionTypeAndPeriodId(
        request.sessionType(), request.periodId())) {
      throw new EntityAlreadyExistsException("EXAM_SESSION_TYPE_ALREADY_EXISTS_IN_THIS_PERIOD");
    }

    // Verify that there is only one OPEN session
    if (request.status() == SessionStatus.OPEN) {
      if (examSessionRepository.existsByStatusAndPeriodId(SessionStatus.OPEN, request.periodId())) {
        throw new ValidationException("AN_OPEN_SESSION_ALREADY_EXISTS_IN_THIS_PERIOD");
      }
    }

    ExamSession sessionToSave = examSessionMapper.toEntity(request);
    return examSessionRepository.save(sessionToSave).getId();
  }

  @Transactional(readOnly = true)
  public List<ExamSessionResponse> getExamSessionsByPeriod(Long periodId) {

    if (!periodRepository.existsById(periodId)) {
      throw new ResourceNotFoundException("PERIOD_NOT_FOUND");
    }

    return examSessionRepository.findByPeriodIdOrderByStartDateAsc(periodId).stream()
        .map(examSessionMapper::toResponse)
        .toList();
  }

  @Transactional
  public ExamSessionResponse updateExamSession(Long id, ExamSessionRequest request) {

    // Retrieve the existing session
    ExamSession existingSession =
        examSessionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SESSION_NOT_FOUND"));

    // Check if the parent period has changed and verify its existence
    if (!existingSession.getPeriod().getId().equals(request.periodId())) {
      if (!periodRepository.existsById(request.periodId())) {
        throw new ResourceNotFoundException("PERIOD_NOT_FOUND");
      }
      existingSession.setPeriod(periodRepository.getReferenceById(request.periodId()));
    }

    // Check label uniqueness for THIS period (excluding the current ID)
    if (examSessionRepository.existsByLabelIgnoreCaseAndPeriodIdAndIdNot(
        request.label(), request.periodId(), id)) {
      throw new EntityAlreadyExistsException("EXAM_SESSION_LABEL_ALREADY_EXISTS_IN_THIS_PERIOD");
    }

    // Check session TYPE uniqueness (excluding the current ID)
    if (examSessionRepository.existsBySessionTypeAndPeriodIdAndIdNot(
        request.sessionType(), request.periodId(), id)) {
      throw new EntityAlreadyExistsException("EXAM_SESSION_TYPE_ALREADY_EXISTS_IN_THIS_PERIOD");
    }

    // Ensure only one session is OPEN (excluding the current ID)
    if (request.status() == SessionStatus.OPEN) {
      if (examSessionRepository.existsByStatusAndPeriodIdAndIdNot(
          SessionStatus.OPEN, request.periodId(), id)) {
        throw new ValidationException("AN_OPEN_SESSION_ALREADY_EXISTS_IN_THIS_PERIOD");
      }
    }

    // Update data via MapStruct
    examSessionMapper.updateEntityFromRequest(request, existingSession);

    // Save
    ExamSession updatedSession = examSessionRepository.save(existingSession);
    return examSessionMapper.toResponse(updatedSession);
  }
}
