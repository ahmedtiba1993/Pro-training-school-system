package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.ContinuousPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.ContinuousPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.ContinuousPromotionResponse;
import com.tiba.pts.modules.trainingsession.mapper.ContinuousPromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.ContinuousPromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContinuousPromotionService {

  private final ContinuousPromotionRepository repository;
  private final ContinuousPromotionMapper mapper;
  private final TrainingRepository trainingRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long createContinuousPromotion(ContinuousPromotionRequest request) {
    // Check that the training exists
    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    // Check the consistency of the training type
    if (training.getTrainingType() != TrainingType.CONTINUOUS) {
      throw new BusinessValidationException("INVALID_TRAINING_TYPE_MUST_BE_CONTINUOUS");
    }

    // Check uniqueness of the code
    if (promotionRepository.existsByCode(request.getCode())) {
      throw new EntityAlreadyExistsException("PROMOTION_CODE_ALREADY_EXISTS");
    }

    // Map the DTO to Entity
    ContinuousPromotion entityToSave = mapper.toEntity(request);

    // Assign the relationship and initial status
    entityToSave.setTraining(training);
    entityToSave.setStatus(PromotionStatus.PLANNED);

    // Save and return the ID
    return repository.save(entityToSave).getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<ContinuousPromotionResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<ContinuousPromotion> pageResult = repository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public ContinuousPromotionResponse getById(Long id) {
    ContinuousPromotion promotion =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));
    return mapper.toResponse(promotion);
  }

  @Transactional
  public Long update(Long id, ContinuousPromotionRequest request) {
    // Retrieve the existing entity
    ContinuousPromotion existingPromotion =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    // Check the uniqueness of the code if it has been modified
    if (!existingPromotion.getCode().equals(request.getCode())
        && promotionRepository.existsByCode(request.getCode())) {
      throw new EntityAlreadyExistsException("PROMOTION_CODE_ALREADY_EXISTS");
    }

    // Check and update the training if it has changed
    if (!existingPromotion.getTraining().getId().equals(request.getTrainingId())) {
      Training training =
          trainingRepository
              .findById(request.getTrainingId())
              .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

      if (training.getTrainingType() != TrainingType.CONTINUOUS) {
        throw new BusinessValidationException("INVALID_TRAINING_TYPE_MUST_BE_CONTINUOUS");
      }
      existingPromotion.setTraining(training);
    }

    // Update the other fields via MapStruct
    mapper.updateEntityFromRequest(request, existingPromotion);

    // Save
    return repository.save(existingPromotion).getId();
  }

  @Transactional
  public void updateStatus(Long id, PromotionStatus newStatus) {
    ContinuousPromotion existingPromotion =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    existingPromotion.setStatus(newStatus);
    repository.save(existingPromotion);
  }

  @Transactional(readOnly = true)
  public List<ContinuousPromotionResponse> getPromotionsByStatus(
      PromotionStatus status, Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<ContinuousPromotion> promotions;

    if (limit != null && limit > 0) {
      // DB optimization: we only request 'limit' results
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions = repository.findByStatus(status, pageable).getContent();
    } else {
      // Retrieves everything
      promotions = repository.findByStatus(status, sort);
    }

    return promotions.stream().map(mapper::toResponse).toList();
  }
}
