package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.domain.enums.SessionType;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.dto.request.ExamSessionRequest;
import com.tiba.pts.modules.academicyear.dto.response.ExamSessionResponse;
import com.tiba.pts.modules.academicyear.mapper.ExamSessionMapper;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
// repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamSessionService {

  private final ExamSessionRepository examSessionRepository;
  private final PeriodRepository periodRepository;
  private final ExamSessionMapper examSessionMapper;

  @Transactional
  public Long createExamSession(ExamSessionRequest request) {
    // Retrieve the Period and its Academic Year
    Period period =
        periodRepository
            .findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("PERIOD_NOT_FOUND"));

    AcademicYear academicYear = period.getAcademicYear();

    // Check the academic year status
    if (academicYear.getStatus() == YearStatus.COMPLETED) {
      throw new BusinessValidationException("CREATION_FORBIDDEN_YEAR_IS_COMPLETED");
    }

    LocalDate reqStart = request.startDate();

    // Rules for the Main Session (MAIN)
    if (request.sessionType() == SessionType.MAIN) {

      // The period must not be locked
      if (Boolean.TRUE.equals(period.getIsLocked())) {
        throw new BusinessValidationException("CANNOT_CREATE_MAIN_SESSION_IN_LOCKED_PERIOD");
      }

      // Uniqueness of the MAIN session
      if (examSessionRepository.existsByPeriodIdAndSessionType(period.getId(), SessionType.MAIN)) {
        throw new EntityAlreadyExistsException("MAIN_SESSION_ALREADY_EXISTS_FOR_THIS_PERIOD");
      }
    }
    // Rules for the Retake Session (RETAKE)
    else if (request.sessionType() == SessionType.RETAKE) {

      // Uniqueness of the RETAKE session
      if (examSessionRepository.existsByPeriodIdAndSessionType(
          period.getId(), SessionType.RETAKE)) {
        throw new EntityAlreadyExistsException("RETAKE_SESSION_ALREADY_EXISTS_FOR_THIS_PERIOD");
      }

      // The MAIN session MUST exist (retrieved to verify dates)
      ExamSession mainSession =
          examSessionRepository
              .findByPeriodIdAndSessionType(period.getId(), SessionType.MAIN)
              .orElseThrow(
                  () ->
                      new BusinessValidationException("CANNOT_CREATE_RETAKE_WITHOUT_MAIN_SESSION"));

      // The retake start date must be AFTER the main session end date
      if (!reqStart.isAfter(mainSession.getEndDate())) {
        throw new BusinessValidationException(
            "RETAKE_START_DATE_MUST_BE_AFTER_MAIN_SESSION_END_DATE");
      }
    }

    // Mapping, lock forcing, and save
    ExamSession session = examSessionMapper.toEntity(request);
    session.setPeriod(period);
    session.setIsLocked(false);

    return examSessionRepository.save(session).getId();
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

    // Retrieve the Session (and deduce the Period and Year)
    ExamSession existingSession =
        examSessionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SESSION_NOT_FOUND"));

    Period period = existingSession.getPeriod();
    AcademicYear academicYear = period.getAcademicYear();

    // Check if the Session itself is locked
    if (Boolean.TRUE.equals(existingSession.getIsLocked())) {
      throw new BusinessValidationException("CANNOT_UPDATE_LOCKED_EXAM_SESSION");
    }

    // Check the Academic Year status
    if (academicYear.getStatus() == YearStatus.COMPLETED) {
      throw new BusinessValidationException("UPDATE_FORBIDDEN_YEAR_IS_COMPLETED");
    }

    LocalDate reqStart = request.startDate();
    LocalDate reqEnd = request.endDate();

    // Update of a Main Session (MAIN)
    if (existingSession.getSessionType() == SessionType.MAIN) {

      // The period must not be locked
      if (Boolean.TRUE.equals(period.getIsLocked())) {
        throw new BusinessValidationException("CANNOT_UPDATE_MAIN_SESSION_IN_LOCKED_PERIOD");
      }

      // The Domino Effect: Check if there is a RETAKE session blocking the new end date
      examSessionRepository
          .findByPeriodIdAndSessionType(period.getId(), SessionType.RETAKE)
          .ifPresent(
              retakeSession -> {
                if (!reqEnd.isBefore(retakeSession.getStartDate())) {
                  throw new BusinessValidationException(
                      "MAIN_SESSION_END_DATE_MUST_BE_BEFORE_RETAKE_START_DATE");
                }
              });
    }
    // Update of a Retake Session (RETAKE)
    else if (existingSession.getSessionType() == SessionType.RETAKE) {

      // The Domino Effect: Check the existing MAIN session
      ExamSession mainSession =
          examSessionRepository
              .findByPeriodIdAndSessionType(period.getId(), SessionType.MAIN)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "DATA_CORRUPTION: RETAKE_EXISTS_BUT_MAIN_IS_MISSING"));

      if (!reqStart.isAfter(mainSession.getEndDate())) {
        throw new BusinessValidationException(
            "RETAKE_START_DATE_MUST_BE_AFTER_MAIN_SESSION_END_DATE");
      }
    }

    // Apply modifications
    examSessionMapper.updateEntityFromRequest(request, existingSession);

    // Save
    ExamSession savedSession = examSessionRepository.save(existingSession);

    return examSessionMapper.toResponse(savedSession);
  }

  /** Toggles the lock state (isLocked) of an exam session. */
  @Transactional
  public void toggleSessionLock(Long id) {
    // Retrieve the session
    ExamSession session =
        examSessionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SESSION_NOT_FOUND"));

    boolean isCurrentlyLocked = Boolean.TRUE.equals(session.getIsLocked());

    if (!isCurrentlyLocked) {
      session.setIsLocked(true);

    } else {
      Period period = session.getPeriod();

      // Check the Academic Year (The Dead Archive)
      if (period.getAcademicYear().getStatus() == YearStatus.COMPLETED) {
        throw new BusinessValidationException("CANNOT_UNLOCK_SESSION_OF_COMPLETED_YEAR");
      }

      // Specific rules for the MAIN session
      if (session.getSessionType() == SessionType.MAIN) {

        // The Period Shield
        if (Boolean.TRUE.equals(period.getIsLocked())) {
          throw new BusinessValidationException("CANNOT_UNLOCK_MAIN_SESSION_IN_LOCKED_PERIOD");
        }

        // The Golden Rule (The Double Lock)
        examSessionRepository
            .findByPeriodIdAndSessionType(period.getId(), SessionType.RETAKE)
            .ifPresent(
                retakeSession -> {
                  if (Boolean.TRUE.equals(retakeSession.getIsLocked())) {
                    throw new BusinessValidationException(
                        "CANNOT_UNLOCK_MAIN_SESSION_WHILE_RETAKE_IS_LOCKED");
                  }
                });
      }

      // If all checks pass, unlock
      session.setIsLocked(false);
    }

    // Save
    examSessionRepository.save(session);
  }
}
