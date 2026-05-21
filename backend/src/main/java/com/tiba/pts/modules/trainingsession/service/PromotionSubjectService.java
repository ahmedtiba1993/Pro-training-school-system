package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.subject.domain.enums.SubjectStatus;
import com.tiba.pts.modules.subject.repository.SubjectRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.*;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.PromotionSubjectRequest;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionStatsResponse;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionSubjectResponse;
import com.tiba.pts.modules.trainingsession.mapper.PromotionSubjectMapper;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionSubjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionSubjectService {

  private final PromotionSubjectRepository promotionSubjectRepository;
  private final PromotionRepository promotionRepository;
  private final SubjectRepository subjectRepository;
  private final PeriodRepository periodRepository;
  private final PromotionSubjectMapper promotionSubjectMapper;

  @Transactional
  public Long create(PromotionSubjectRequest request) {

    // Retrieval and verification of the Promotion's existence
    Promotion promotion =
        promotionRepository
            .findById(request.getPromotionId())
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    // Promotion status verification (Safeguard)
    if (promotion.getStatus() == PromotionStatus.IN_PROGRESS
        || promotion.getStatus() == PromotionStatus.EVALUATION
        || promotion.getStatus() == PromotionStatus.COMPLETED) {
      throw new BusinessValidationException("PROMOTION_STATUS_FORBIDS_SUBJECT_ADDITION");
    }

    // Retrieval and verification of the Subject
    Subject subject =
        subjectRepository
            .findById(request.getSubjectId())
            .orElseThrow(() -> new EntityNotFoundException("SUBJECT_NOT_FOUND"));

    // The Subject must strictly be ACTIVE
    if (subject.getStatus() != SubjectStatus.ACTIVE) {
      throw new BusinessValidationException("SUBJECT_MUST_BE_ACTIVE");
    }

    // Validation of Specialties match (DDD Invariant)
    Long subjectSpecialtyId = subject.getTraining().getSpecialty().getId();
    Long promotionSpecialtyId = promotion.getTraining().getSpecialty().getId();
    if (!subjectSpecialtyId.equals(promotionSpecialtyId)) {
      throw new BusinessValidationException("SPECIALTY_MISMATCH_BETWEEN_SUBJECT_AND_PROMOTION");
    }

    Period academicPeriod = null;

    // Specific logic according to the polymorphic type of the Promotion
    if (promotion instanceof AccreditedPromotion accreditedPromotion) {
      // --- Case: Accredited Promotion ---
      if (request.getAcademicPeriodId() == null) {
        throw new BusinessValidationException(
            "ACADEMIC_PERIOD_ID_REQUIRED_FOR_ACCREDITED_PROMOTION");
      }

      academicPeriod =
          periodRepository
              .findById(request.getAcademicPeriodId())
              .orElseThrow(() -> new EntityNotFoundException("ACADEMIC_PERIOD_NOT_FOUND"));

      // The chosen period must belong to the academic year of this promotion
      if (!academicPeriod
          .getAcademicYear()
          .getId()
          .equals(accreditedPromotion.getAcademicYear().getId())) {
        throw new BusinessValidationException("PERIOD_DOES_NOT_BELONG_TO_PROMOTION_ACADEMIC_YEAR");
      }

      // COMMON AND SIMPLIFIED UNIQUENESS CHECK (just with promotionId and subjectId)
      // Blocks if the subject already exists in any semester of this year (Accredited)
      // or anywhere in the course (Accelerated/Continuous).
      boolean alreadyExists =
          promotionSubjectRepository.existsByPromotionIdAndSubjectId(
              promotion.getId(), subject.getId());

      if (alreadyExists) {
        throw new EntityAlreadyExistsException("SUBJECT_ALREADY_ASSIGNED_TO_THIS_PROMOTION");
      }

    } else if (promotion instanceof AcceleratedPromotion
        || promotion instanceof ContinuousPromotion) {
      // --- Case: Bootcamp / E-learning ---
      // Force to null according to specifications (training in a single block)
      if (request.getAcademicPeriodId() != null) {
        throw new BusinessValidationException("ACADEMIC_PERIOD_FORBIDDEN_FOR_THIS_PROMOTION_TYPE");
      }

      // Absolute uniqueness check over the entire training
      boolean alreadyExists =
          promotionSubjectRepository.existsByPromotionIdAndSubjectId(
              promotion.getId(), subject.getId());
      if (alreadyExists) {
        throw new EntityAlreadyExistsException("SUBJECT_ALREADY_ASSIGNED_TO_THIS_PROMOTION");
      }
    }

    // Construction and persistence of the temporally fixed binding entity
    PromotionSubject promotionSubject =
        PromotionSubject.builder()
            .promotion(promotion)
            .subject(subject)
            .academicPeriod(academicPeriod)
            .coefficient(request.getCoefficient())
            .build();

    return promotionSubjectRepository.save(promotionSubject).getId();
  }

  /** Retrieves the refined list of subjects assigned to a specific promotion. */
  public List<PromotionSubjectResponse> getSubjectsByPromotion(Long promotionId) {
    // Check if promotion exists (Exception handling)
    if (!promotionRepository.existsById(promotionId)) {
      throw new EntityNotFoundException("PROMOTION_NOT_FOUND");
    }

    // Retrieval and automatic mapping via MapStruct
    return promotionSubjectMapper.toResponseList(
        promotionSubjectRepository.findByPromotionId(promotionId));
  }

  /** Calculates and returns the overall statistical summary for all types of promotions. */
  public PromotionStatsResponse getPromotionStats(Long promotionId) {
    // Retrieval and verification of the promotion
    Promotion promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    // Retrieval of assigned subject relations
    List<PromotionSubject> promotionSubjects =
        promotionSubjectRepository.findByPromotionId(promotionId);

    // Aggregation calculations
    long totalSubjects = promotionSubjects.size();

    double totalCoef =
        promotionSubjects.stream().mapToDouble(PromotionSubject::getCoefficient).sum();

    double globalHourlyVolume =
        promotionSubjects.stream().mapToDouble(ps -> ps.getSubject().getTotalHours()).sum();

    // Dynamic determination of type and specific attributes
    String type = null;
    String academicYearLabel = null;

    if (promotion instanceof AccreditedPromotion accreditedPromotion) {
      type = "ACCREDITED";
      academicYearLabel = accreditedPromotion.getAcademicYear().getLabel();
    } else if (promotion instanceof AcceleratedPromotion) {
      type = "ACCELERATED";
    } else if (promotion instanceof ContinuousPromotion) {
      type = "CONTINUOUS";
    }

    // Construction of the unified statistics DTO
    return PromotionStatsResponse.builder()
        .promotionName(promotion.getName())
        .promotionType(type)
        .academicYearLabel(academicYearLabel)
        .totalSubjects(totalSubjects)
        .totalCoefficient(totalCoef)
        .globalHourlyVolume(globalHourlyVolume)
        .trainingLabel(
            promotion.getTraining().getLevel().getCode()
                + "-"
                + promotion.getTraining().getSpecialty().getLabel())
        .build();
  }

  /** Retrieves the list of subjects of a promotion filtered by a specific academic period. */
  public List<PromotionSubjectResponse> getSubjectsByPromotionAndPeriod(
      Long promotionId, Long periodId) {

    // Verify if the promotion exists
    if (!promotionRepository.existsById(promotionId)) {
      throw new EntityNotFoundException("PROMOTION_NOT_FOUND");
    }

    // Verify if the period exists
    if (!periodRepository.existsById(periodId)) {
      throw new EntityNotFoundException("ACADEMIC_PERIOD_NOT_FOUND");
    }

    // Retrieval of filtered data
    List<PromotionSubject> promotionSubjects =
        promotionSubjectRepository.findByPromotionIdAndAcademicPeriodId(promotionId, periodId);

    // Automatic mapping to response DTOs
    return promotionSubjectMapper.toResponseList(promotionSubjects);
  }

  /**
   * Deletes the assignment of a subject to a promotion. The action is STRICTLY forbidden if the
   * promotion's status is not DRAFT.
   */
  @Transactional
  public void delete(Long id) {
    // Retrieval and verification of the binding's existence
    PromotionSubject promotionSubject =
        promotionSubjectRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_SUBJECT_NOT_FOUND"));

    // Retrieval of the parent promotion
    Promotion promotion = promotionSubject.getPromotion();

    // Regulatory safeguard: Only allowed if the status is DRAFT
    if (promotion.getStatus() != PromotionStatus.DRAFT) {
      throw new BusinessValidationException("PROMOTION_STATUS_FORBIDS_SUBJECT_DELETION");
    }

    // Physical deletion of the binding
    promotionSubjectRepository.delete(promotionSubject);
  }
}
