package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionUpdateRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionResponse;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionStatsResponse;
import com.tiba.pts.modules.trainingsession.dto.response.OngoingPromotionResponse;
import com.tiba.pts.modules.trainingsession.mapper.AccreditedPromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.AccreditedPromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.projection.PromotionStatusStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccreditedPromotionService {

  private final AccreditedPromotionRepository accreditedPromotionRepository;
  private final AccreditedPromotionMapper mapper;
  private final TrainingRepository trainingRepository;
  private final AcademicYearRepository academicYearRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long createAccreditedPromotion(AccreditedPromotionRequest request) {

    // Verify if the training exists
    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    // Verify if the academic year exists
    AcademicYear academicYear =
        academicYearRepository
            .findById(request.getAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Verify the academic year status
    if (academicYear.getStatus() != YearStatus.PLANNED
        && academicYear.getStatus() != YearStatus.ENROLLMENT
        && academicYear.getStatus() != YearStatus.IN_PROGRESS) {
      throw new BusinessValidationException("ACADEMIC_YEAR_MUST_BE_ACTIVE");
    }

    // Verify the training type
    if (training.getTrainingType() != TrainingType.ACCREDITED) {
      throw new BusinessValidationException("TRAINING_MUST_BE_ACCREDITED_TYPE");
    }

    // Verify the training status (must be ACTIVE)
    if (training.getStatus() != TrainingStatus.ACTIVE) {
      throw new BusinessValidationException("TRAINING_MUST_BE_ACTIVE");
    }

    // Verify the uniqueness of the combination [training_id + academic_year_id]
    if (accreditedPromotionRepository.existsByTrainingIdAndAcademicYearId(
        training.getId(), academicYear.getId())) {
      throw new EntityAlreadyExistsException("ACCREDITED_PROMOTION_ALREADY_EXISTS_FOR_THIS_YEAR");
    }

    // Mapping (Using MapStruct)
    AccreditedPromotion promotion = mapper.toEntity(request);

    // Attaching relationships
    promotion.setTraining(training);
    promotion.setAcademicYear(academicYear);

    // Dynamic Code generation
    String yearSuffix = generateYearSuffix(academicYear.getLabel());
    String baseCode =
        training.getCode().toUpperCase()
            + "-"
            + yearSuffix; // We count the existing promotions for this training that year
    long existingCount =
        accreditedPromotionRepository.countByTrainingAndAcademicYear(
            training.getId(), academicYear.getId());
    // We add a numerical suffix (e.g.: CAP-ELC-HOM-2526-01)
    String generatedCode = String.format("%s-%02d", baseCode, existingCount + 1);
    promotion.setCode(generatedCode);

    // Force default business values
    promotion.setStatus(PromotionStatus.DRAFT);
    promotion.setEnrollmentCount(0);

    // Save
    AccreditedPromotion savedPromotion = accreditedPromotionRepository.save(promotion);

    return savedPromotion.getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<AccreditedPromotionResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<AccreditedPromotion> pageResult = accreditedPromotionRepository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AccreditedPromotionResponse getById(Long id) {
    AccreditedPromotion promotion =
        accreditedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));
    return mapper.toResponse(promotion);
  }

  @Transactional
  public void updateAccreditedPromotion(Long id, AccreditedPromotionUpdateRequest request) {

    // Retrieve the entity
    AccreditedPromotion promotion =
        accreditedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    // THE SAFEGUARD (The Read-Only rule)
    if (promotion.getStatus() == PromotionStatus.COMPLETED
        || promotion.getStatus() == PromotionStatus.CANCELLED) {
      throw new BusinessValidationException("CANNOT_CHANGE");
    }

    // Pricing Rule: Forbidden to modify if enrollments are ongoing
    if (promotion.getEnrollmentCount() > 0
        && request.registrationFee().compareTo(promotion.getRegistrationFee()) != 0) {
      throw new BusinessValidationException(
          "CANNOT_CHANGE_REGISTRATION_FEE_WITH_ACTIVE_ENROLLMENTS");
    }
    if (promotion.getEnrollmentCount() > 0
        && request.tuitionFee().compareTo(promotion.getTuitionFee()) != 0) {
      throw new BusinessValidationException("CANNOT_CHANGE_TUITION_FEE_WITH_ACTIVE_ENROLLMENTS");
    }

    // Anti-Eviction Rule: Capacity
    if (request.capacity() < promotion.getEnrollmentCount()) {
      throw new BusinessValidationException("CAPACITY_CANNOT_BE_LESS_THAN_ENROLLMENT_COUNT");
    }

    // Update via MapStruct (which will now copy the registration dates)
    mapper.updateEntity(request, promotion);
  }

  @Transactional(readOnly = true)
  public List<AccreditedPromotionResponse> getPromotionsByStatus(
      PromotionStatus status, Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<AccreditedPromotion> promotions;

    if (limit != null && limit > 0) {
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions = accreditedPromotionRepository.findByStatus(status, pageable).getContent();
    } else {
      promotions = accreditedPromotionRepository.findByStatus(status, sort);
    }

    return promotions.stream().map(mapper::toResponse).toList();
  }

  @Transactional
  public void changeStatus(Long id, PromotionStatus newStatus) {
    AccreditedPromotion promotion =
        accreditedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    PromotionStatus currentStatus = promotion.getStatus();

    if (currentStatus == newStatus) {
      throw new BusinessValidationException("PROMOTION_ALREADY_IN_REQUESTED_STATUS");
    }

    switch (newStatus) {
      case ENROLLMENT:
        if (currentStatus != PromotionStatus.DRAFT) {
          throw new BusinessValidationException("INVALID_TRANSITION_TO_ENROLLMENT");
        }
        break;
      case CANCELLED:
        if (currentStatus != PromotionStatus.DRAFT && currentStatus != PromotionStatus.ENROLLMENT) {
          throw new BusinessValidationException("INVALID_TRANSITION_TO_CANCELLED");
        }
        break;
      default:
        throw new BusinessValidationException("UNKNOWN_STATUS_TRANSITION");
    }
    promotion.setStatus(newStatus);
    accreditedPromotionRepository.save(promotion);
  }

  @Transactional
  public void syncStatusWithAcademicYear(Long yearId, YearStatus newYearStatus) {
    PromotionStatus targetStatus;
    switch (newYearStatus) {
      case ENROLLMENT:
        targetStatus = PromotionStatus.ENROLLMENT;
        break;
      case IN_PROGRESS:
        targetStatus = PromotionStatus.IN_PROGRESS;
        break;
      case CLOSING:
        targetStatus = PromotionStatus.EVALUATION;
      case COMPLETED:
        targetStatus = PromotionStatus.COMPLETED;
        break;
      default:
        return;
    }
    accreditedPromotionRepository.updateStatusByAcademicYearId(targetStatus, yearId);
  }

  @Transactional(readOnly = true)
  public List<AccreditedPromotionStatsResponse> getPromotionStatistics() {

    List<PromotionStatus> targetStatuses =
        List.of(
            PromotionStatus.DRAFT,
            PromotionStatus.ENROLLMENT,
            PromotionStatus.IN_PROGRESS,
            PromotionStatus.EVALUATION);

    // Optimized retrieval in a single query
    List<PromotionStatusStatsProjection> projections =
        accreditedPromotionRepository.getStatsByStatuses(targetStatuses);

    // EnumMap
    Map<PromotionStatus, PromotionStatusStatsProjection> statsMap =
        new EnumMap<>(PromotionStatus.class);
    for (PromotionStatusStatsProjection proj : projections) {
      statsMap.put(proj.getStatus(), proj);
    }

    // Building the final response, with safe handling of empty statuses
    return targetStatuses.stream()
        .map(
            status -> {
              PromotionStatusStatsProjection proj = statsMap.get(status);
              long count = (proj != null) ? proj.getPromotionCount() : 0L;
              long enrollments = (proj != null) ? proj.getTotalEnrollments() : 0L;

              return new AccreditedPromotionStatsResponse(status, count, enrollments);
            })
        .toList();
  }

  /**
   * Retrieves the list of promotions with ENROLLMENT and IN_PROGRESS statuses. Returns a
   * lightweight version (OngoingPromotionResponse).
   *
   * @param limit The maximum number of elements to return (can be null)
   * @return List of OngoingPromotionResponse
   */
  @Transactional(readOnly = true)
  public List<OngoingPromotionResponse> getOngoingPromotions(Integer limit) {
    List<PromotionStatus> targetStatuses =
        List.of(PromotionStatus.ENROLLMENT, PromotionStatus.IN_PROGRESS);

    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<AccreditedPromotion> promotions;

    if (limit != null && limit > 0) {
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions =
          accreditedPromotionRepository.findByStatusIn(targetStatuses, pageable).getContent();
    } else {
      promotions = accreditedPromotionRepository.findByStatusIn(targetStatuses, sort);
    }

    // Using MapStruct to transform into OngoingPromotionResponse
    return promotions.stream().map(mapper::toOngoingResponse).toList();
  }

  private String generateYearSuffix(String academicYearLabel) {
    if (academicYearLabel == null || academicYearLabel.isBlank()) {
      return "YY";
    }
    try {
      String[] parts = academicYearLabel.split("-");
      if (parts.length == 2) {
        String start = parts[0].trim().substring(parts[0].trim().length() - 2);
        String end = parts[1].trim().substring(parts[1].trim().length() - 2);
        return start + end;
      }
    } catch (Exception e) {
      return academicYearLabel.replaceAll("[^0-9]", "");
    }
    return academicYearLabel.replaceAll("[^0-9]", "");
  }
}
