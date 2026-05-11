package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.dto.request.TrainingRequest;
import com.tiba.pts.modules.specialty.dto.response.TrainingResponse;
import com.tiba.pts.modules.specialty.dto.response.TrainingTypeCountResponse;
import com.tiba.pts.modules.specialty.mapper.TrainingMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
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
public class TrainingService {

  private final TrainingRepository trainingRepository;
  private final TrainingMapper trainingMapper;
  private final LevelRepository levelRepository;
  private final SpecialtyRepository specialtyRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long createTraining(TrainingRequest request) {
    Level level = getActiveLevelOrThrow(request.levelId());
    Specialty specialty = getSpecialtyOrThrow(request.specialtyId());

    validateDurationRules(request.trainingType(), request.durationUnit());
    checkUniqueness(level.getId(), specialty.getId(), request.trainingType(), null);

    String generatedCode =
        generateTechnicalCode(level.getCode(), specialty.getCode(), request.trainingType());

    Training training = trainingMapper.toEntity(request);
    training.setLevel(level);
    training.setSpecialty(specialty);
    training.setCode(generatedCode);

    if (training.getStatus() == null) {
      training.setStatus(TrainingStatus.DRAFT);
    }

    return trainingRepository.save(training).getId();
  }

  public PageResponse<TrainingResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<Training> pageResult = trainingRepository.findAll(pageable);
    return PageResponse.of(pageResult, trainingMapper::toResponse);
  }

  public List<TrainingResponse> getAllActive(TrainingType type) {
    List<Training> activeTrainings =
        (type != null)
            ? trainingRepository.findAllByStatusAndTrainingType(TrainingStatus.ACTIVE, type)
            : trainingRepository.findAllByStatus(TrainingStatus.ACTIVE);

    return activeTrainings.stream().map(trainingMapper::toResponse).toList();
  }

  public List<TrainingResponse> getActiveTrainingsByLevelId(Long levelId, TrainingType type) {
    // Check level existence
    if (!levelRepository.existsById(levelId)) {
      throw new ResourceNotFoundException("LEVEL_NOT_FOUND");
    }

    // Conditional retrieval
    List<Training> trainings;
    if (type != null) {
      trainings =
          trainingRepository.findAllByStatusAndLevelIdAndTrainingType(
              TrainingStatus.ACTIVE, levelId, type);
    } else {
      trainings = trainingRepository.findAllByStatusAndLevelId(TrainingStatus.ACTIVE, levelId);
    }

    return trainings.stream().map(trainingMapper::toResponse).toList();
  }

  public List<TrainingTypeCountResponse> getActiveTrainingStats() {
    return trainingRepository.countTrainingsByTypeAndStatus(TrainingStatus.ACTIVE).stream()
        .map(p -> new TrainingTypeCountResponse(p.getTrainingType(), p.getCount()))
        .toList();
  }

  @Transactional
  public Long updateTraining(Long id, TrainingRequest request) {
    Training existingTraining =
        trainingRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    TrainingStatus currentStatus = existingTraining.getStatus();

    //  ARCHIVED -> Update total
    if (currentStatus == TrainingStatus.ARCHIVED) {
      return trainingRepository.save(existingTraining).getId();
    }

    // ACTIVE / SUSPENDED -> High supervision
    if (currentStatus == TrainingStatus.ACTIVE || currentStatus == TrainingStatus.SUSPENDED) {
      if (isCoreFieldsModified(existingTraining, request)) {
        throw new BusinessValidationException("ACTIVE_TRAINING_CORE_FIELDS_LOCKED");
      }
      existingTraining.setDescription(request.description());
      return trainingRepository.save(existingTraining).getId();
    }

    // Check if there are promotions linked to this training
    boolean hasPromotions = promotionRepository.existsByTrainingId(existingTraining.getId());
    if (hasPromotions) {
      throw new BusinessValidationException("PROMOTIONS_CANNOT_CHANGE_TYPE");
    }

    // Full freedom
    if (currentStatus == TrainingStatus.DRAFT) {
      Level level = getActiveLevelOrThrow(request.levelId());
      Specialty specialty = getSpecialtyOrThrow(request.specialtyId());

      validateDurationRules(request.trainingType(), request.durationUnit());
      checkUniqueness(level.getId(), specialty.getId(), request.trainingType(), id);

      // Manually update fields
      existingTraining.setDescription(request.description());
      existingTraining.setTrainingType(request.trainingType());
      existingTraining.setDurationValue(request.durationValue());
      existingTraining.setDurationUnit(request.durationUnit());
      existingTraining.setLevel(level);
      existingTraining.setSpecialty(specialty);

      return trainingRepository.save(existingTraining).getId();
    }

    return existingTraining.getId();
  }

  @Transactional
  public Long updateStatus(Long id, TrainingStatus newStatus) {
    if (newStatus == null) {
      throw new BusinessValidationException("TRAINING_STATUS_REQUIRED");
    }

    Training training =
        trainingRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    training.setStatus(newStatus);
    return trainingRepository.save(training).getId();
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================

  private Level getActiveLevelOrThrow(Long levelId) {
    Level level =
        levelRepository
            .findById(levelId)
            .orElseThrow(() -> new ResourceNotFoundException("LEVEL_NOT_FOUND"));
    if (!level.getIsActive()) {
      throw new BusinessValidationException("LEVEL_IS_NOT_ACTIVE");
    }
    return level;
  }

  private Specialty getSpecialtyOrThrow(Long specialtyId) {
    return specialtyRepository
        .findById(specialtyId)
        .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));
  }

  private void checkUniqueness(Long levelId, Long specialtyId, TrainingType type, Long excludeId) {
    boolean exists =
        (excludeId == null)
            ? trainingRepository.existsByLevelIdAndSpecialtyIdAndTrainingType(
                levelId, specialtyId, type)
            : trainingRepository.existsByLevelIdAndSpecialtyIdAndTrainingTypeAndIdNot(
                levelId, specialtyId, type, excludeId);

    if (exists) {
      throw new EntityAlreadyExistsException("TRAINING_COMBINATION_ALREADY_EXISTS");
    }
  }

  private boolean isCoreFieldsModified(Training existing, TrainingRequest request) {
    return !existing.getLevel().getId().equals(request.levelId())
        || !existing.getSpecialty().getId().equals(request.specialtyId())
        || existing.getTrainingType() != request.trainingType()
        || !existing.getDurationValue().equals(request.durationValue())
        || existing.getDurationUnit() != request.durationUnit();
  }

  private void validateDurationRules(TrainingType type, DurationUnit unit) {
    switch (type) {
      case ACCELERATED -> {
        if (unit != DurationUnit.HOURS && unit != DurationUnit.DAYS) {
          throw new BusinessValidationException("INVALID_CONTINUOUS_DURATION_UNIT");
        }
      }
      case ACCREDITED -> {
        if (unit != DurationUnit.MONTHS && unit != DurationUnit.YEARS) {
          throw new BusinessValidationException("INVALID_ACCREDITED_DURATION_UNIT");
        }
      }
      case CONTINUOUS -> {
        if (unit != DurationUnit.MONTHS && unit != DurationUnit.WEEKS) {
          throw new BusinessValidationException("INVALID_ACCELERATED_DURATION_UNIT");
        }
      }
      default -> throw new BusinessValidationException("UNKNOWN_TRAINING_TYPE");
    }
  }

  private String generateTechnicalCode(String levelCode, String specialtyCode, TrainingType type) {
    String typePrefix =
        switch (type) {
          case CONTINUOUS -> "CON";
          case ACCREDITED -> "HOM";
          case ACCELERATED -> "ACC";
        };
    return String.format("%s-%s-%s", levelCode, specialtyCode, typePrefix).toUpperCase();
  }
}
