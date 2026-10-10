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
import org.springframework.data.domain.Pageable;
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

  @Transactional(readOnly = true)
  public PageResponse<TrainingResponse> getAllPaged(Pageable pageable) {
    Page<Training> pageResult = trainingRepository.findAll(pageable);
    return PageResponse.of(pageResult, trainingMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public TrainingResponse getById(Long id) {
    return trainingRepository
        .findById(id)
        .map(trainingMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_TRAINING_NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public List<TrainingResponse> getAllActive(TrainingType type) {
    List<Training> activeTrainings =
        (type != null)
            ? trainingRepository.findAllByStatusAndTrainingType(TrainingStatus.ACTIVE, type)
            : trainingRepository.findAllByStatus(TrainingStatus.ACTIVE);

    return activeTrainings.stream().map(trainingMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<TrainingResponse> getActiveTrainingsByLevelId(Long levelId, TrainingType type) {
    if (!levelRepository.existsById(levelId)) {
      throw new ResourceNotFoundException("SPECIALTY_LEVEL_NOT_FOUND");
    }

    List<Training> trainings =
        (type != null)
            ? trainingRepository.findAllByStatusAndLevelIdAndTrainingType(
                TrainingStatus.ACTIVE, levelId, type)
            : trainingRepository.findAllByStatusAndLevelId(TrainingStatus.ACTIVE, levelId);

    return trainings.stream().map(trainingMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
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
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_TRAINING_NOT_FOUND"));

    TrainingStatus currentStatus = existingTraining.getStatus();

    if (currentStatus == TrainingStatus.ARCHIVED) {
      throw new BusinessValidationException("SPECIALTY_TRAINING_ARCHIVED_CANNOT_BE_MODIFIED");
    }

    if (currentStatus == TrainingStatus.ACTIVE || currentStatus == TrainingStatus.SUSPENDED) {
      if (isCoreFieldsModified(existingTraining, request)) {
        throw new BusinessValidationException("SPECIALTY_TRAINING_ACTIVE_CORE_FIELDS_LOCKED");
      }
      existingTraining.setDescription(request.description());
      return trainingRepository.save(existingTraining).getId();
    }

    boolean hasPromotions = promotionRepository.existsByTrainingId(existingTraining.getId());
    if (hasPromotions && existingTraining.getTrainingType() != request.trainingType()) {
      throw new BusinessValidationException("SPECIALTY_TRAINING_PROMOTIONS_EXIST_TYPE_LOCKED");
    }

    if (currentStatus == TrainingStatus.DRAFT) {
      Level level = getActiveLevelOrThrow(request.levelId());
      Specialty specialty = getSpecialtyOrThrow(request.specialtyId());

      validateDurationRules(request.trainingType(), request.durationUnit());
      checkUniqueness(level.getId(), specialty.getId(), request.trainingType(), id);

      String generatedCode =
          generateTechnicalCode(level.getCode(), specialty.getCode(), request.trainingType());

      existingTraining.setDescription(request.description());
      existingTraining.setTrainingType(request.trainingType());
      existingTraining.setDurationValue(request.durationValue());
      existingTraining.setDurationUnit(request.durationUnit());
      existingTraining.setLevel(level);
      existingTraining.setSpecialty(specialty);
      existingTraining.setCode(generatedCode);

      return trainingRepository.save(existingTraining).getId();
    }

    return existingTraining.getId();
  }

  @Transactional
  public Long updateStatus(Long id, TrainingStatus newStatus) {
    if (newStatus == null) {
      throw new BusinessValidationException("SPECIALTY_TRAINING_STATUS_REQUIRED");
    }

    Training training =
        trainingRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_TRAINING_NOT_FOUND"));

    training.setStatus(newStatus);
    return trainingRepository.save(training).getId();
  }

  @Transactional(readOnly = true)
  public List<TrainingResponse> getTrainingsByLevelAndStatuses(
      Long levelId, List<TrainingStatus> statuses) {
    if (!levelRepository.existsById(levelId)) {
      throw new ResourceNotFoundException("SPECIALTY_LEVEL_NOT_FOUND");
    }

    List<Training> trainings =
        (statuses == null || statuses.isEmpty())
            ? trainingRepository.findAllByLevelId(levelId)
            : trainingRepository.findAllByLevelIdAndStatusIn(levelId, statuses);

    return trainings.stream().map(trainingMapper::toResponse).toList();
  }

  private Level getActiveLevelOrThrow(Long levelId) {
    Level level =
        levelRepository
            .findById(levelId)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_LEVEL_NOT_FOUND"));
    if (!Boolean.TRUE.equals(level.getIsActive())) {
      throw new BusinessValidationException("SPECIALTY_LEVEL_NOT_ACTIVE");
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
      throw new EntityAlreadyExistsException("SPECIALTY_TRAINING_COMBINATION_ALREADY_EXISTS");
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
          throw new BusinessValidationException("SPECIALTY_TRAINING_INVALID_ACCELERATED_DURATION_UNIT");
        }
      }
      case ACCREDITED -> {
        if (unit != DurationUnit.MONTHS && unit != DurationUnit.YEARS) {
          throw new BusinessValidationException("SPECIALTY_TRAINING_INVALID_ACCREDITED_DURATION_UNIT");
        }
      }
      case CONTINUOUS -> {
        if (unit != DurationUnit.MONTHS && unit != DurationUnit.WEEKS) {
          throw new BusinessValidationException("SPECIALTY_TRAINING_INVALID_CONTINUOUS_DURATION_UNIT");
        }
      }
      default -> throw new BusinessValidationException("SPECIALTY_TRAINING_UNKNOWN_TYPE");
    }
  }

  String generateTechnicalCode(String levelCode, String specialtyCode, TrainingType type) {
    if (levelCode == null || specialtyCode == null || type == null) {
      throw new BusinessValidationException("SPECIALTY_TRAINING_CODE_GENERATION_FAILED");
    }
    String cleanedLevelCode = levelCode.trim().toUpperCase();
    String cleanedSpecialtyCode = specialtyCode.trim().toUpperCase();
    String typePrefix = type.getPrefix().trim().toUpperCase();

    return String.format("%s-%s-%s", cleanedLevelCode, cleanedSpecialtyCode, typePrefix);
  }
}
