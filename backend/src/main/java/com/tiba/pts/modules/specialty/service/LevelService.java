package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.dto.request.LevelRequest;
import com.tiba.pts.modules.specialty.dto.response.LevelResponse;
import com.tiba.pts.modules.specialty.mapper.LevelMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LevelService {

  private final LevelRepository levelRepository;
  private final LevelMapper levelMapper;

  public Long create(LevelRequest request) {
    validateLevel(request, null);
    Level levelEntity = levelMapper.toEntity(request);
    return levelRepository.save(levelEntity).getId();
  }

  public List<LevelResponse> getAll() {
    return levelRepository.findAll().stream().map(levelMapper::toResponse).toList();
  }

  public Long update(Long id, LevelRequest request) {
    Level existingLevel =
        levelRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LEVEL_NOT_FOUND"));

    validateLevel(request, id);

    levelMapper.updateEntityFromDto(request, existingLevel);
    return levelRepository.save(existingLevel).getId();
  }

  public Long updateStatus(Long id, boolean isActive) {
    Level existingLevel =
        levelRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("LEVEL_NOT_FOUND"));

    existingLevel.setIsActive(isActive);
    return levelRepository.save(existingLevel).getId();
  }

  // --- MULTIPLE VALIDATION ---
  private void validateLevel(LevelRequest request, Long id) {
    List<ErrorDetail> conflicts = new ArrayList<>();

    // Code verification
    boolean codeExists =
        (id == null)
            ? levelRepository.existsByCode(request.code())
            : levelRepository.existsByCodeAndIdNot(request.code(), id);

    if (codeExists) {
      conflicts.add(new ErrorDetail("code", "CODE_ALREADY_EXISTS"));
    }

    // Label verification
    boolean labelExists =
        (id == null)
            ? levelRepository.existsByLabel(request.label())
            : levelRepository.existsByLabelAndIdNot(request.label(), id);

    if (labelExists) {
      conflicts.add(new ErrorDetail("label", "LABEL_ALREADY_EXISTS"));
    }

    if (!conflicts.isEmpty()) {
      throw new DuplicateResourceException("CONFLICT_DETECTED", conflicts);
    }
  }
}
