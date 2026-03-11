package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.dto.SpecialtyRequest;
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

    // Global business validation
    validateSpecialtyCreation(request, levels);

    // Mapping and association
    Specialty specialty = specialtyMapper.toEntity(request);
    specialty.getAssociatedLevels().addAll(levels);

    return specialtyRepository.save(specialty).getId();
  }

  private void validateSpecialtyCreation(SpecialtyRequest request, List<Level> levels) {
    List<ErrorDetail> erreurs = new ArrayList<>();

    // Check: the name already exist
    if (specialtyRepository.existsByNameIgnoreCase(request.getName())) {
      erreurs.add(new ErrorDetail("name", "SPECIALTY_NAME_ALREADY_EXISTS"));
    }

    // Check: the code already exist
    if (specialtyRepository.existsByCodeIgnoreCase(request.getCode())) {
      erreurs.add(new ErrorDetail("code", "SPECIALTY_CODE_ALREADY_EXISTS"));
    }

    // Check: Do all provided IDs exist in the database
    if (levels.size() != request.getLevelIds().size()) {
      erreurs.add(new ErrorDetail("levelIds", "ONE_OR_MORE_LEVELS_NOT_FOUND"));
    }

    // If there is at least one error, block the process
    if (!erreurs.isEmpty()) {
      throw new DuplicateResourceException("VALIDATION_ERREUR", erreurs);
    }
  }
}
