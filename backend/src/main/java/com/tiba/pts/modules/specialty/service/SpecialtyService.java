package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.dto.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.SpecialtyResponse;
import com.tiba.pts.modules.specialty.mapper.SpecialtyMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

  private final SpecialtyRepository specialtyRepository;
  private final LevelRepository levelRepository;
  private final SpecialtyMapper specialtyMapper;

  @Transactional
  public Long createSpecialty(SpecialtyRequest request) {
    Set<Long> requestedLevelIds = request.getLevelIds();
    List<Level> levels = levelRepository.findAllById(requestedLevelIds);

    // Pour la création, on passe 'null' comme ID
    validateSpecialty(null, request, levels);

    Specialty specialty = specialtyMapper.toEntity(request);
    specialty.getAssociatedLevels().addAll(levels);

    return specialtyRepository.save(specialty).getId();
  }

  @Transactional
  public Long updateSpecialty(Long id, SpecialtyRequest request) {
    Specialty specialty =
        specialtyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));

    // Extract the level IDs
    Set<Long> requestedLevelIds = request.getLevelIds();
    List<Level> levels = levelRepository.findAllById(requestedLevelIds);

    // Validate the specialty data
    validateSpecialty(id, request, levels);

    // Update basic fields of the specialty
    specialty.setName(request.getName());
    specialty.setCode(request.getCode());

    // Clear existing associated levels to avoid duplicates or outdated relations
    specialty.getAssociatedLevels().clear();
    specialty.getAssociatedLevels().addAll(levels);

    return specialtyRepository.save(specialty).getId();
  }

  @Transactional(readOnly = true)
  public List<SpecialtyResponse> getAllSpecialties() {
    List<Specialty> specialties = specialtyRepository.findAllWithLevels();
    return specialtyMapper.toResponseList(specialties);
  }

  @Transactional(readOnly = true)
  public List<SpecialtyResponse> getSpecialtiesByLevelId(Long levelId) {
    List<Specialty> specialties = specialtyRepository.findByAssociatedLevels_Id(levelId);
    return specialtyMapper.toResponseList(specialties);
  }

  private void validateSpecialty(Long specialtyId, SpecialtyRequest request, List<Level> levels) {
    List<ErrorDetail> erreurs = new ArrayList<>();

    // Check if the specialty name already exists
    // If specialtyId is null → creation case
    // Otherwise → update case, excluding the current specialty ID
    boolean nameExists =
        (specialtyId == null)
            ? specialtyRepository.existsByNameIgnoreCase(request.getName())
            : specialtyRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), specialtyId);

    if (nameExists) {
      erreurs.add(new ErrorDetail("name", "SPECIALTY_NAME_ALREADY_EXISTS"));
    }

    // Check if the specialty code already exists
    // Same logic: exclude the current ID during update
    boolean codeExists =
        (specialtyId == null)
            ? specialtyRepository.existsByCodeIgnoreCase(request.getCode())
            : specialtyRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), specialtyId);

    if (codeExists) {
      erreurs.add(new ErrorDetail("code", "SPECIALTY_CODE_ALREADY_EXISTS"));
    }

    // Verify that all provided level IDs actually exist in the database
    if (levels.size() != request.getLevelIds().size()) {
      erreurs.add(new ErrorDetail("levelIds", "ONE_OR_MORE_LEVELS_NOT_FOUND"));
    }

    // Throw a validation exception if any errors were found
    if (!erreurs.isEmpty()) {
      throw new DuplicateResourceException("VALIDATION_ERREUR", erreurs);
    }
  }
}
