package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.dto.request.LevelRequest;
import com.tiba.pts.modules.specialty.dto.response.LevelResponse;
import com.tiba.pts.modules.specialty.mapper.LevelMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LevelService {

  private final LevelRepository levelRepository;
  private final LevelMapper levelMapper;

  @Transactional
  public Long create(LevelRequest request) {
    validateLevelUniqueness(request, null);
    Level levelEntity = levelMapper.toEntity(request);
    return levelRepository.save(levelEntity).getId();
  }

  @Transactional(readOnly = true)
  public List<LevelResponse> getAll() {
    return levelRepository.findAll().stream().map(levelMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public LevelResponse getById(Long id) {
    return levelRepository
        .findById(id)
        .map(levelMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_LEVEL_NOT_FOUND"));
  }

  @Transactional
  public Long update(Long id, LevelRequest request) {
    Level existingLevel =
        levelRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_LEVEL_NOT_FOUND"));

    validateLevelUniqueness(request, id);

    levelMapper.updateEntityFromRequest(request, existingLevel);
    return levelRepository.save(existingLevel).getId();
  }

  @Transactional
  public Long updateStatus(Long id, boolean isActive) {
    Level existingLevel =
        levelRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_LEVEL_NOT_FOUND"));

    existingLevel.setIsActive(isActive);
    return levelRepository.save(existingLevel).getId();
  }

  @Transactional(readOnly = true)
  public List<LevelResponse> getAllActive() {
    return levelRepository.findByIsActiveTrue().stream().map(levelMapper::toResponse).toList();
  }

  private void validateLevelUniqueness(LevelRequest request, Long id) {
    boolean codeExists =
        (id == null)
            ? levelRepository.existsByCode(request.code())
            : levelRepository.existsByCodeAndIdNot(request.code(), id);

    if (codeExists) {
      throw new EntityAlreadyExistsException("SPECIALTY_LEVEL_CODE_ALREADY_EXISTS");
    }

    boolean labelExists =
        (id == null)
            ? levelRepository.existsByLabel(request.label())
            : levelRepository.existsByLabelAndIdNot(request.label(), id);

    if (labelExists) {
      throw new EntityAlreadyExistsException("SPECIALTY_LEVEL_LABEL_ALREADY_EXISTS");
    }
  }
}
