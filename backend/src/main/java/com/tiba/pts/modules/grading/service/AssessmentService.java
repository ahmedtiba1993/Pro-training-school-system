package com.tiba.pts.modules.grading.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.examscheduling.repository.ExamTimetableRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import com.tiba.pts.modules.grading.dto.request.AssessmentRequest;
import com.tiba.pts.modules.grading.dto.response.AssessmentLookupResponse;
import com.tiba.pts.modules.grading.dto.response.AssessmentResponse;
import com.tiba.pts.modules.grading.mapper.AssessmentMapper;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.repository.PromotionSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

  private final AssessmentRepository assessmentRepository;
  private final ExamTimetableRepository examTimetableRepository;
  private final AssessmentMapper assessmentMapper;
  private final PromotionSubjectRepository promotionSubjectRepository;

  @Transactional
  public Long createAssessment(AssessmentRequest request) {

    PromotionSubject promotionSubject =
        promotionSubjectRepository
            .findById(request.promotionSubjectId())
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_SUBJECT_NOT_FOUND"));

    // Validation: Cannot add assessment if promotion is COMPLETED or CANCELLED
    Promotion promotion = promotionSubject.getPromotion();
    if (promotion != null
        && (promotion.getStatus() == PromotionStatus.COMPLETED
            || promotion.getStatus() == PromotionStatus.CANCELLED)) {
      throw new BusinessValidationException("PROMOTION_STATUS_FORBIDS_ASSESSMENT_CREATION");
    }

    // Validation: Only one assessment per type allowed for a given promotion subject (ignoring CANCELLED)
    if (assessmentRepository.existsByPromotionSubjectIdAndAssessmentTypeAndStatusNot(
        request.promotionSubjectId(), request.assessmentType(), AssessmentStatus.CANCELLED)) {
      throw new EntityAlreadyExistsException("ASSESSMENT_ALREADY_EXISTS_FOR_TYPE");
    }

    // Validation: Title must be unique for a given promotion subject
    if (assessmentRepository.existsByPromotionSubjectIdAndTitleIgnoreCase(
        request.promotionSubjectId(), request.title())) {
      throw new EntityAlreadyExistsException("ASSESSMENT_TITLE_ALREADY_EXISTS");
    }

    int weightPercentage = request.weightPercentage();
    if (request.assessmentType() == AssessmentType.RETAKE) {
      weightPercentage = 0;
    }

    // Validation: The sum of weight percentages for the same promotion subject must not exceed 100%
    Integer currentSum =
        assessmentRepository.sumWeightPercentageByPromotionSubjectId(request.promotionSubjectId());
    if (currentSum + weightPercentage > 100) {
      throw new BusinessValidationException("WEIGHT_PERCENTAGE_SUM_EXCEEDS_100");
    }

    Assessment assessment = assessmentMapper.toEntity(request);
    assessment.setPromotionSubject(promotionSubject);
    assessment.setWeightPercentage(weightPercentage);
    assessment.setStatus(AssessmentStatus.DRAFT);

    return assessmentRepository.save(assessment).getId();
  }

  @Transactional
  public Long updateAssessment(Long id, AssessmentRequest request) {

    // Find existing assessment
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    // Golden rule: Only assessments in DRAFT or PLANNED status can be modified
    if (assessment.getStatus() != AssessmentStatus.PLANNED
        && assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new BusinessValidationException("ASSESSMENT_CAN_ONLY_BE_UPDATED_IN_PLANNED_STATUS");
    }

    // Validation: Verify parent promotion status
    Promotion promotion = assessment.getPromotionSubject().getPromotion();
    if (promotion != null
        && (promotion.getStatus() == PromotionStatus.COMPLETED
            || promotion.getStatus() == PromotionStatus.CANCELLED)) {
      throw new BusinessValidationException("PROMOTION_STATUS_FORBIDS_ASSESSMENT_UPDATE");
    }

    Long promotionSubjectId = assessment.getPromotionSubject().getId();

    // Validation: Type uniqueness (if type changed, verify another one does not already exist, excluding CANCELLED)
    if (assessment.getAssessmentType() != request.assessmentType()
        && assessmentRepository.existsByPromotionSubjectIdAndAssessmentTypeAndStatusNotAndIdNot(
            promotionSubjectId, request.assessmentType(), AssessmentStatus.CANCELLED, id)) {
      throw new EntityAlreadyExistsException("ASSESSMENT_ALREADY_EXISTS_FOR_TYPE");
    }

    // Validation: Title uniqueness (case-insensitive and excluding current record)
    if (!assessment.getTitle().equalsIgnoreCase(request.title())
        && assessmentRepository.existsByPromotionSubjectIdAndTitleIgnoreCaseAndIdNot(
            promotionSubjectId, request.title(), id)) {
      throw new EntityAlreadyExistsException("ASSESSMENT_TITLE_ALREADY_EXISTS");
    }

    // Retake handling: weight forced to 0 so as not to distort the 100% total
    int newWeightPercentage = request.weightPercentage();
    if (request.assessmentType() == AssessmentType.RETAKE) {
      newWeightPercentage = 0;
    }

    // Validation: Recalculate sum (Current Sum - Old Weight + New Weight <= 100)
    Integer currentSum =
        assessmentRepository.sumWeightPercentageByPromotionSubjectId(promotionSubjectId);
    if (currentSum == null) {
      currentSum = 0;
    }

    int oldWeight =
        (assessment.getAssessmentType() == AssessmentType.RETAKE
                || assessment.getStatus() == AssessmentStatus.CANCELLED)
            ? 0
            : assessment.getWeightPercentage();
    int recalculatedSum = (currentSum - oldWeight) + newWeightPercentage;

    if (recalculatedSum > 100) {
      throw new BusinessValidationException("WEIGHT_PERCENTAGE_SUM_EXCEEDS_100");
    }

    // Apply modifications (promotionSubject remains immutable)
    assessment.setTitle(request.title().trim());
    assessment.setAssessmentType(request.assessmentType());
    assessment.setTotalMarks(request.totalMarks());
    assessment.setWeightPercentage(newWeightPercentage);

    return assessmentRepository.save(assessment).getId();
  }

  @Transactional(readOnly = true)
  public List<AssessmentResponse> getAssessmentsByPromotionSubject(Long promotionSubjectId) {
    if (!promotionSubjectRepository.existsById(promotionSubjectId)) {
      throw new ResourceNotFoundException("PROMOTION_SUBJECT_NOT_FOUND");
    }

    return assessmentRepository.findByPromotionSubjectId(promotionSubjectId).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AssessmentResponse> getGradingAssessmentsByPromotionSubject(Long promotionSubjectId) {
    if (!promotionSubjectRepository.existsById(promotionSubjectId)) {
      throw new ResourceNotFoundException("PROMOTION_SUBJECT_NOT_FOUND");
    }

    return assessmentRepository
        .findByPromotionSubjectIdAndStatus(promotionSubjectId, AssessmentStatus.GRADING_IN_PROGRESS)
        .stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AssessmentLookupResponse> getUnscheduledAssessmentsForTimetable(Long timetableId) {
    ExamTimetable timetable =
        examTimetableRepository
            .findById(timetableId)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIMETABLE_NOT_FOUND"));

    Long classId = timetable.getClassGroup().getId();

    return assessmentRepository
        .findUnscheduledAssessmentsForTimetable(classId, timetableId)
        .stream()
        .map(assessmentMapper::toLookupResponse)
        .toList();
  }

  @Transactional
  public void deleteAssessment(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new BusinessValidationException("ASSESSMENT_CAN_ONLY_BE_DELETED_IN_DRAFT_STATUS");
    }

    assessmentRepository.delete(assessment);
  }

  @Transactional
  public void markAsPlanned(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new BusinessValidationException("ASSESSMENT_NOT_IN_DRAFT_STATUS");
    }

    validatePromotionNotClosed(assessment.getPromotionSubject().getPromotion());

    assessment.setStatus(AssessmentStatus.PLANNED);
    assessmentRepository.save(assessment);
  }

  @Transactional
  public void openForGrading(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.PLANNED) {
      throw new BusinessValidationException("ASSESSMENT_NOT_IN_PLANNED_STATUS");
    }

    validatePromotionNotClosed(assessment.getPromotionSubject().getPromotion());

    assessment.setStatus(AssessmentStatus.GRADING_IN_PROGRESS);
    assessmentRepository.save(assessment);
  }

  @Transactional
  public void submitToAdmin(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.GRADING_IN_PROGRESS) {
      throw new BusinessValidationException("ASSESSMENT_NOT_IN_GRADING_STATUS");
    }

    assessment.setStatus(AssessmentStatus.SUBMITTED_TO_ADMIN);
    assessmentRepository.save(assessment);
  }

  @Transactional
  public void rejectAndReturnToTeacher(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.SUBMITTED_TO_ADMIN) {
      throw new BusinessValidationException("ASSESSMENT_NOT_SUBMITTED_TO_ADMIN");
    }

    assessment.setStatus(AssessmentStatus.GRADING_IN_PROGRESS);
    assessmentRepository.save(assessment);
  }

  @Transactional
  public void publishAssessment(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.SUBMITTED_TO_ADMIN) {
      throw new BusinessValidationException("ASSESSMENT_NOT_SUBMITTED_TO_ADMIN");
    }

    assessment.setStatus(AssessmentStatus.PUBLISHED);
    assessmentRepository.save(assessment);
  }

  @Transactional
  public void cancelAssessment(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() == AssessmentStatus.LOCKED) {
      throw new BusinessValidationException("CANNOT_CANCEL_LOCKED_ASSESSMENT");
    }
    if (assessment.getStatus() == AssessmentStatus.CANCELLED) {
      throw new BusinessValidationException("ASSESSMENT_ALREADY_CANCELLED");
    }

    assessment.setStatus(AssessmentStatus.CANCELLED);
    assessment.setWeightPercentage(0);
    assessmentRepository.save(assessment);
  }

  @Transactional
  public void lockAssessment(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
      throw new BusinessValidationException("ASSESSMENT_NOT_IN_PUBLISHED_STATUS");
    }

    assessment.setStatus(AssessmentStatus.LOCKED);
    assessmentRepository.save(assessment);
  }

  private void validatePromotionNotClosed(Promotion promotion) {
    if (promotion != null
        && (promotion.getStatus() == PromotionStatus.COMPLETED
            || promotion.getStatus() == PromotionStatus.CANCELLED)) {
      throw new BusinessValidationException("PROMOTION_STATUS_FORBIDS_OPERATION");
    }
  }
}
