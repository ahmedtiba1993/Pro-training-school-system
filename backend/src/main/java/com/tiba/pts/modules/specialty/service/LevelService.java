package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.dto.LevelDto;
import com.tiba.pts.modules.specialty.mapper.LevelMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelService {

  private final LevelRepository levelRepository;
  private final LevelMapper levelMapper;

  public Long create(LevelDto dto) {
    validateLevel(dto, null);
    Level levelEntity = levelMapper.toEntity(dto);
    return levelRepository.save(levelEntity).getId();
  }

  public List<LevelDto> getAll() {
    return levelRepository.findAll().stream()
        .map(levelMapper::toReponse)
        .collect(Collectors.toList());
  }

  private void validateLevel(LevelDto request, Long id) {
    List<ErrorDetail> conflicts = new ArrayList<>();
    boolean exists =
        (id == null)
            ? levelRepository.existsByCode(request.getCode()) // create
            : levelRepository.existsByCodeAndIdNot(request.getCode(), id); // update
    if (exists) {
      conflicts.add(new ErrorDetail("code", "CODE_ALREADY_EXISTS"));
    }

    if (!conflicts.isEmpty()) {
      throw new DuplicateResourceException("CONFLICT_DETECTED", conflicts);
    }
  }
}
