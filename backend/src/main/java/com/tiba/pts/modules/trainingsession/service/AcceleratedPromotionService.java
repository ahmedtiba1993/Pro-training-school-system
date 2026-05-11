package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AcceleratedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.DurationUnit;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.AcceleratedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AcceleratedPromotionResponse;
import com.tiba.pts.modules.trainingsession.mapper.AcceleratedPromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.AcceleratedPromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcceleratedPromotionService {

  private final AcceleratedPromotionRepository acceleratedPromotionRepository;
  private final AcceleratedPromotionMapper mapper;
  private final TrainingRepository trainingRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long create(AcceleratedPromotionRequest request) {

    // Check existence of linked Training
    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    // Training must be active
    if (training.getStatus() == null || !training.getStatus().name().equals("ACTIVE")) {
      throw new BusinessValidationException("TRAINING_MUST_BE_ACTIVE");
    }

    // Training must necessarily be of type ACCELERATED
    if (training.getTrainingType() != TrainingType.ACCELERATED) {
      throw new BusinessValidationException("TRAINING_TYPE_MUST_BE_ACCELERATED");
    }

    // Automatic and intelligent SKU generation
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
    String baseCode =
        String.format("%s-%s", training.getCode(), request.getStartDate().format(formatter));
    String uniqueCode = generateUniqueCode(baseCode);

    // Mapping and forcing system rules
    AcceleratedPromotion promotion = mapper.toEntity(request);
    promotion.setTraining(training);
    promotion.setCode(uniqueCode);
    promotion.setStatus(PromotionStatus.DRAFT);
    promotion.setEnrollmentCount(0);
    promotion.setDurationUnit(DurationUnit.valueOf(training.getDurationUnit().name()));
    promotion.setDurationValue(training.getDurationValue());

    return promotionRepository.save(promotion).getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<AcceleratedPromotionResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<AcceleratedPromotion> pageResult = acceleratedPromotionRepository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AcceleratedPromotionResponse getById(Long id) {
    AcceleratedPromotion promotion =
        acceleratedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));
    return mapper.toResponse(promotion);
  }

  @Transactional
  public Long update(Long id, AcceleratedPromotionRequest request) {

    // Retrieve existing entity
    AcceleratedPromotion promotion =
        acceleratedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    // Business rule: Cannot be modified if COMPLETED or CANCELLED
    if (promotion.getStatus() == PromotionStatus.COMPLETED
        || promotion.getStatus() == PromotionStatus.CANCELLED) {
      throw new BusinessValidationException("PROMOTION_CANNOT_BE_MODIFIED_IN_CURRENT_STATUS");
    }

    // Business rule: If enrolled > 0, prohibition to modify fees
    if (promotion.getEnrollmentCount() > 0) {
      boolean isRegistrationFeeChanged =
          request.getRegistrationFee().compareTo(promotion.getRegistrationFee()) != 0;
      boolean isTuitionFeeChanged =
          request.getTuitionFee().compareTo(promotion.getTuitionFee()) != 0;

      if (isRegistrationFeeChanged || isTuitionFeeChanged) {
        throw new BusinessValidationException("CANNOT_MODIFY_FEES_WITH_ACTIVE_ENROLLMENTS");
      }
    }

    // Business rule: If IN_PROGRESS, formal prohibition to change startDate
    if (promotion.getStatus() == PromotionStatus.IN_PROGRESS) {
      if (!request.getStartDate().isEqual(promotion.getStartDate())) {
        throw new BusinessValidationException("CANNOT_MODIFY_START_DATE_WHEN_IN_PROGRESS");
      }
    }

    // Apply allowed modifications via MapStruct
    mapper.updateEntityFromRequest(request, promotion);

    // Return the response
    return promotionRepository.save(promotion).getId();
  }

  @Transactional(readOnly = true)
  public List<AcceleratedPromotionResponse> getPromotionsByStatus(
      PromotionStatus status, Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<AcceleratedPromotion> promotions;

    if (limit != null && limit > 0) {
      // DB optimization: we only request 'limit' results
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions = acceleratedPromotionRepository.findByStatus(status, pageable).getContent();
    } else {
      // Retrieves everything
      promotions = acceleratedPromotionRepository.findByStatus(status, sort);
    }

    return promotions.stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public Map<PromotionStatus, Long> getSpecificStatusCounts() {
    List<PromotionStatus> targetStatuses =
        List.of(
            PromotionStatus.DRAFT,
            PromotionStatus.ENROLLMENT,
            PromotionStatus.IN_PROGRESS,
            PromotionStatus.EVALUATION);

    Map<PromotionStatus, Long> statusCounts = new EnumMap<>(PromotionStatus.class);

    for (PromotionStatus status : targetStatuses) {
      statusCounts.put(status, acceleratedPromotionRepository.countByStatus(status));
    }

    return statusCounts;
  }

  @Transactional
  public void changeStatus(Long id, PromotionStatus status) {

    AcceleratedPromotion promotion =
        acceleratedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    PromotionStatus currentStatus = promotion.getStatus();

    // If status is identical, return directly
    if (currentStatus == status) {
      return;
    }

    // Check business transition rules
    if (!isValidStatusTransition(currentStatus, status)) {
      throw new BusinessValidationException("INVALID_STATUS_TRANSITION");
    }

    promotion.setStatus(status);
    promotionRepository.save(promotion);
  }

  /**
   * Simplified state machine for promotion status transitions. Expected workflow:
   * DRAFT -> ENROLLMENT -> IN_PROGRESS -> EVALUATION -> COMPLETED
   */
  private boolean isValidStatusTransition(PromotionStatus current, PromotionStatus target) {
    if (current == null || target == null) return false;

    return switch (current) {
      case DRAFT -> target == PromotionStatus.ENROLLMENT || target == PromotionStatus.CANCELLED;
      case ENROLLMENT ->
          target == PromotionStatus.IN_PROGRESS || target == PromotionStatus.CANCELLED;
      case IN_PROGRESS ->
          target == PromotionStatus.EVALUATION || target == PromotionStatus.CANCELLED;
      case EVALUATION -> target == PromotionStatus.COMPLETED;
      case COMPLETED, CANCELLED -> false;
      default -> false;
    };
  }

  /**
   * Business method to automatically resolve SKU collisions. If "DEV-10-2023" exists,
   * returns "DEV-10-20231", etc.
   */
  private String generateUniqueCode(String baseCode) {
    String finalCode = baseCode;
    int counter = 0;

    // While code exists in DB, increment the suffix
    while (promotionRepository.existsByCodeIgnoreCase(finalCode)) {
      finalCode = baseCode + counter;
      counter++;
    }

    return finalCode;
  }
}
