package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionResponse;
import com.tiba.pts.modules.trainingsession.mapper.AccreditedPromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.AccreditedPromotionRepository;
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
public class AccreditedPromotionService {

  private final AccreditedPromotionRepository repository;
  private final AccreditedPromotionMapper mapper;
  private final TrainingRepository trainingRepository;
  private final AcademicYearRepository academicYearRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long createAccreditedPromotion(AccreditedPromotionRequest request) {
    // Check that the training exists
    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    // Check that the academic year exists
    AcademicYear academicYear =
        academicYearRepository
            .findById(request.getAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Check the consistency of the training type
    if (training.getTrainingType() != TrainingType.ACCREDITED) {
      throw new BusinessValidationException("INVALID_TRAINING_TYPE_MUST_BE_ACCREDITED");
    }

    // Check uniqueness of the code
    if (promotionRepository.existsByCode(request.getCode())) {
      throw new EntityAlreadyExistsException("PROMOTION_CODE_ALREADY_EXISTS");
    }

    // Map the DTO to Entity
    AccreditedPromotion entityToSave = mapper.toEntity(request);

    // Assign the relationships and initial status
    entityToSave.setTraining(training);
    entityToSave.setAcademicYear(academicYear);
    entityToSave.setStatus(PromotionStatus.PLANNED);

    // Save and return the ID
    return repository.save(entityToSave).getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<AccreditedPromotionResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
    Page<AccreditedPromotion> pageResult = repository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AccreditedPromotionResponse getById(Long id) {
    AccreditedPromotion promotion =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));
    return mapper.toResponse(promotion);
  }

  @Transactional
  public Long update(Long id, AccreditedPromotionRequest request) {
    // Retrieve the existing entity
    AccreditedPromotion existingPromotion =
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

      if (training.getTrainingType() != TrainingType.ACCREDITED) {
        throw new BusinessValidationException("INVALID_TRAINING_TYPE_MUST_BE_ACCREDITED");
      }
      existingPromotion.setTraining(training);
    }

    // Check and update the academic year if it has changed
    if (!existingPromotion.getAcademicYear().getId().equals(request.getAcademicYearId())) {
      AcademicYear academicYear =
          academicYearRepository
              .findById(request.getAcademicYearId())
              .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));
      existingPromotion.setAcademicYear(academicYear);
    }

    // Update the other fields via MapStruct
    mapper.updateEntityFromRequest(request, existingPromotion);

    // Save
    return repository.save(existingPromotion).getId();
  }

  @Transactional
  public void updateStatus(Long id, PromotionStatus newStatus) {
    AccreditedPromotion existingPromotion =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    existingPromotion.setStatus(newStatus);
    repository.save(existingPromotion);
  }

  @Transactional(readOnly = true)
  public List<AccreditedPromotionResponse> getPromotionsByStatus(
      PromotionStatus status, Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<AccreditedPromotion> promotions;

    if (limit != null && limit > 0) {
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions = repository.findByStatus(status, pageable).getContent();
    } else {
      promotions = repository.findByStatus(status, sort);
    }

    return promotions.stream().map(mapper::toResponse).toList();
  }
}
