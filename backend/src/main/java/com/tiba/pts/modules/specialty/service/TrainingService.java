package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.dto.request.TrainingRequest;
import com.tiba.pts.modules.specialty.dto.response.TrainingResponse;
import com.tiba.pts.modules.specialty.dto.response.TrainingTypeCountResponse;
import com.tiba.pts.modules.specialty.mapper.TrainingMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.specialty.repository.projection.TrainingTypeCountProjection;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingService {

  private final TrainingRepository trainingRepository;
  private final TrainingMapper trainingMapper;
  private final LevelRepository levelRepository;
  private final SpecialtyRepository specialtyRepository;

  @Transactional
  public Long createTraining(TrainingRequest requestDTO) {

    // Method to check for uniqueness
    boolean alreadyExists =
        trainingRepository.existsByLevelIdAndSpecialtyId(
            requestDTO.getLevelId(), requestDTO.getSpecialtyId());

    if (alreadyExists) {
      throw new EntityAlreadyExistsException("training already exists.");
    }

    // Fetch and associate the Level ans specilaity
    Level level =
        levelRepository
            .findById(requestDTO.getLevelId())
            .orElseThrow(() -> new ResourceNotFoundException("Level not found"));
    Specialty specialty =
        specialtyRepository
            .findById(requestDTO.getSpecialtyId())
            .orElseThrow(() -> new ResourceNotFoundException("Specialty not found"));

    // Convert basic info
    Training training = trainingMapper.toEntity(requestDTO);

    training.setLevel(level);
    training.setSpecialty(specialty);

    return trainingRepository.save(training).getId();
  }

  public PageResponse<TrainingResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
    Page<Training> pageResult = trainingRepository.findAll(pageable);
    return PageResponse.of(pageResult, trainingMapper::toResponse);
  }

  public List<TrainingResponse> getAllActive() {
    return trainingRepository.findByIsActiveTrue().stream()
        .map(trainingMapper::toResponse)
        .toList();
  }

  @Transactional
  public boolean changeActivation(Long id) {
    Training training =
        trainingRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Training not found with ID: " + id));
    training.setActive(!training.isActive());
    trainingRepository.save(training);
    return training.isActive();
  }

  @Transactional
  public Long updateTraining(Long id, TrainingRequest requestDTO) {
    // Verify that the training exists
    Training existingTraining =
        trainingRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Training not found with ID: " + id));

    // Check for uniqueness
    boolean alreadyExists =
        trainingRepository.existsByLevelIdAndSpecialtyIdAndIdNot(
            requestDTO.getLevelId(), requestDTO.getSpecialtyId(), id);

    if (alreadyExists) {
      throw new EntityAlreadyExistsException(
          "Another training with this level and specialty already exists.");
    }

    // Fetch Level and Specialty
    Level level =
        levelRepository
            .findById(requestDTO.getLevelId())
            .orElseThrow(() -> new ResourceNotFoundException("Level not found"));

    Specialty specialty =
        specialtyRepository
            .findById(requestDTO.getSpecialtyId())
            .orElseThrow(() -> new ResourceNotFoundException("Specialty not found"));

    // Update simple fields VIA THE MAPPER
    trainingMapper.updateEntityFromRequest(requestDTO, existingTraining);

    // Manually update JPA relationships
    existingTraining.setLevel(level);
    existingTraining.setSpecialty(specialty);

    // Save
    return trainingRepository.save(existingTraining).getId();
  }

  public List<TrainingTypeCountResponse> getActiveTrainingStats() {

    List<TrainingTypeCountProjection> projections = trainingRepository.countActiveTrainingsByType();

    // Use the Mapper to transform Projections into DTOs
    List<TrainingTypeCountResponse> mappedResults =
        trainingMapper.toTrainingTypeCountResponseList(projections);

    // Convert to a temporary Map to check for missing types
    Map<TrainingType, Long> countsMap =
        mappedResults.stream()
            .collect(
                Collectors.toMap(
                    TrainingTypeCountResponse::getTrainingType,
                    TrainingTypeCountResponse::getCount));

    // Build the final list ensuring all statuses exist (defaulting to 0)
    List<TrainingTypeCountResponse> finalResult = new ArrayList<>();
    for (TrainingType type : TrainingType.values()) {
      finalResult.add(new TrainingTypeCountResponse(type, countsMap.getOrDefault(type, 0L)));
    }

    return finalResult;
  }
}
