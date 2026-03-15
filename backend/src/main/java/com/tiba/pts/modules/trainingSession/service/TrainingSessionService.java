package com.tiba.pts.modules.trainingSession.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import com.tiba.pts.modules.trainingSession.domain.entity.TrainingSession;
import com.tiba.pts.modules.trainingSession.dto.TrainingSessionRequest;
import com.tiba.pts.modules.trainingSession.dto.TrainingSessionResponse;
import com.tiba.pts.modules.trainingSession.mapper.TrainingSessionMapper;
import com.tiba.pts.modules.trainingSession.repository.TrainingSessionRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingSessionService {

  private final TrainingSessionRepository trainingSessionRepository;
  private final AcademicYearRepository academicYearRepository;
  private final LevelRepository levelRepository;
  private final SpecialtyRepository specialtyRepository;
  private final TrainingSessionMapper trainingSessionMapper;

  @Transactional
  public Long addTrainingSession(TrainingSessionRequest request) {
    validateTrainingSession(request, null);
    TrainingSession entity = trainingSessionMapper.toEntity(request);
    return trainingSessionRepository.save(entity).getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainingSessionResponse> getPaginated(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<TrainingSession> pageResult = trainingSessionRepository.findAll(pageable);
    return PageResponse.of(pageResult, trainingSessionMapper::toResponse);
  }

  @Transactional
  public Long updateTrainingSession(Long id, TrainingSessionRequest request) {
    TrainingSession existingSession =
        trainingSessionRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("TRAINING SESSION NOT FOUND WITH ID: " + id));

    validateTrainingSession(request, id);
    trainingSessionMapper.updateEntityFromRequest(request, existingSession);
    return trainingSessionRepository.save(existingSession).getId();
  }

  private void validateTrainingSession(TrainingSessionRequest request, Long id) {
    // Check the existence of the relationships
    if (!academicYearRepository.existsById(request.getAcademicYearId())) {
      throw new ResourceNotFoundException("ACADEMIC YEAR NOT FOUND");
    }
    if (!levelRepository.existsById(request.getLevelId())) {
      throw new ResourceNotFoundException("LEVEL NOT FOUND");
    }
    if (!specialtyRepository.existsById(request.getSpecialtyId())) {
      throw new ResourceNotFoundException("SPECIALTY NOT FOUND");
    }

    // Check the association between Specialty and Level
    if (!specialtyRepository.existsSpecialtyLevelAssociation(
        request.getSpecialtyId(), request.getLevelId())) {
      throw new ValidationException(
          "THE SELECTED LEVEL IS NOT ASSOCIATED WITH THE GIVEN SPECIALTY");
    }

    List<ErrorDetail> conflicts = new ArrayList<>();

    // Validate uniqueness of the promotion name
    boolean promotionExists =
        (id == null)
            ? trainingSessionRepository.existsByPromotionNameIgnoreCase(request.getPromotionName())
            : trainingSessionRepository.existsByPromotionNameIgnoreCaseAndIdNot(
                request.getPromotionName(), id);

    if (promotionExists) {
      conflicts.add(new ErrorDetail("promotionName", "PROMOTION_NAME_ALREADY_EXISTS"));
    }

    // Validate uniqueness of the triplet (AcademicYear, Level, Specialty)
    boolean combinationExists =
        (id == null)
            ? trainingSessionRepository.existsByAcademicYearIdAndLevelIdAndSpecialtyId(
                request.getAcademicYearId(), request.getLevelId(), request.getSpecialtyId())
            : trainingSessionRepository.existsByAcademicYearIdAndLevelIdAndSpecialtyIdAndIdNot(
                request.getAcademicYearId(), request.getLevelId(), request.getSpecialtyId(), id);

    if (combinationExists) {
      conflicts.add(
          new ErrorDetail("trainingSession", "TRAINING_SESSION_COMBINATION_ALREADY_EXISTS"));
    }

    if (!conflicts.isEmpty()) {
      throw new DuplicateResourceException("CONFLICT_DETECTED", conflicts);
    }
  }
}
