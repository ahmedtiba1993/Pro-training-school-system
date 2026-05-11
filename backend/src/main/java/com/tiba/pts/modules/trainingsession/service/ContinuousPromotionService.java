package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.ContinuousPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.DurationUnit;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.ContinuousPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.ContinuousPromotionResponse;
import com.tiba.pts.modules.trainingsession.mapper.ContinuousPromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.ContinuousPromotionRepository;
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
public class ContinuousPromotionService {

  private final ContinuousPromotionRepository continuousPromotionRepository;
  private final ContinuousPromotionMapper mapper;
  private final TrainingRepository trainingRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long create(ContinuousPromotionRequest request) {
    if (continuousPromotionRepository.existsByNameIgnoreCase(request.getName())) {
      throw new EntityAlreadyExistsException("PROMOTION_NAME_ALREADY_EXISTS");
    }

    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new EntityNotFoundException("TRAINING_NOT_FOUND"));

    if (training.getStatus() == null || !training.getStatus().name().equals("ACTIVE")) {
      throw new BusinessValidationException("TRAINING_MUST_BE_ACTIVE");
    }

    if (training.getTrainingType() != TrainingType.CONTINUOUS) {
      throw new BusinessValidationException("TRAINING_TYPE_MUST_BE_CONTINUOUS");
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
    String baseCode =
        String.format("%s-%s", training.getCode(), request.getStartDate().format(formatter));
    String uniqueCode = generateUniqueCode(baseCode);

    ContinuousPromotion promotion = mapper.toEntity(request);
    promotion.setTraining(training);
    promotion.setCode(uniqueCode);
    promotion.setStatus(PromotionStatus.DRAFT);
    promotion.setEnrollmentCount(0);
    promotion.setDurationUnit(DurationUnit.valueOf(training.getDurationUnit().name()));
    promotion.setDurationValue(training.getDurationValue());

    return continuousPromotionRepository.save(promotion).getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<ContinuousPromotionResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<ContinuousPromotion> pageResult = continuousPromotionRepository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public ContinuousPromotionResponse getById(Long id) {
    ContinuousPromotion promotion =
        continuousPromotionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));
    return mapper.toResponse(promotion);
  }

  @Transactional
  public Long update(Long id, ContinuousPromotionRequest request) {
    ContinuousPromotion promotion =
        continuousPromotionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    if (!promotion.getName().equalsIgnoreCase(request.getName())
        && continuousPromotionRepository.existsByNameIgnoreCase(request.getName())) {
      throw new EntityAlreadyExistsException("PROMOTION_NAME_ALREADY_EXISTS");
    }

    if (promotion.getStatus() == PromotionStatus.COMPLETED
        || promotion.getStatus() == PromotionStatus.CANCELLED) {
      throw new BusinessValidationException("PROMOTION_CANNOT_BE_MODIFIED_IN_CURRENT_STATUS");
    }

    if (promotion.getEnrollmentCount() > 0) {
      boolean isRegistrationFeeChanged =
          request.getRegistrationFee().compareTo(promotion.getRegistrationFee()) != 0;
      boolean isTuitionFeeChanged =
          request.getTuitionFee().compareTo(promotion.getTuitionFee()) != 0;

      if (isRegistrationFeeChanged || isTuitionFeeChanged) {
        throw new BusinessValidationException("CANNOT_MODIFY_FEES_WITH_ACTIVE_ENROLLMENTS");
      }
    }

    if (promotion.getStatus() == PromotionStatus.IN_PROGRESS
        && !request.getStartDate().isEqual(promotion.getStartDate())) {
      throw new BusinessValidationException("CANNOT_MODIFY_START_DATE_WHEN_IN_PROGRESS");
    }

    mapper.updateEntityFromRequest(request, promotion);

    return continuousPromotionRepository.save(promotion).getId();
  }

  @Transactional(readOnly = true)
  public List<ContinuousPromotionResponse> getPromotionsByStatus(
      PromotionStatus status, Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<ContinuousPromotion> promotions;

    if (limit != null && limit > 0) {
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions = continuousPromotionRepository.findByStatus(status, pageable).getContent();
    } else {
      promotions = continuousPromotionRepository.findByStatus(status, sort);
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
      statusCounts.put(status, continuousPromotionRepository.countByStatus(status));
    }

    return statusCounts;
  }

  @Transactional
  public void changeStatus(Long id, PromotionStatus status) {
    ContinuousPromotion promotion =
        continuousPromotionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    PromotionStatus currentStatus = promotion.getStatus();
    if (currentStatus == status) return;

    if (!isValidStatusTransition(currentStatus, status)) {
      throw new BusinessValidationException("INVALID_STATUS_TRANSITION");
    }

    promotion.setStatus(status);
    continuousPromotionRepository.save(promotion);
  }

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

  private String generateUniqueCode(String baseCode) {
    String finalCode = baseCode;
    int counter = 0;
    while (promotionRepository.existsByCodeIgnoreCase(finalCode)) {
      finalCode = baseCode + counter;
      counter++;
    }
    return finalCode;
  }
}
