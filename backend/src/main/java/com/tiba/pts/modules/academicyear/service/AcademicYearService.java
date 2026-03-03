package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.dto.AcademicYearDto;
import com.tiba.pts.modules.academicyear.mapper.AcademicYearMapper;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearService {

  private final AcademicYearRepository repository;
  private final AcademicYearMapper mapper;

  public Long create(AcademicYearDto request) {
    validateAcademicYear(request, null);
    AcademicYear entity = mapper.toEntity(request);
    return repository.save(entity).getId();
  }

  private void validateAcademicYear(AcademicYearDto request, Long id) {
    List<ErrorDetail> conflicts = new ArrayList<>();
    boolean exists =
        (id == null)
            ? repository.existsByLabel(request.getLabel()) // create
            : repository.existsByLabelAndIdNot(request.getLabel(), id); // update

    if (exists) {
      conflicts.add(new ErrorDetail("label", "LABEL_ALREADY_EXISTS"));
    }

    if (!conflicts.isEmpty()) {
      throw new DuplicateResourceException("CONFLICT_DETECTED", conflicts);
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<AcademicYearDto> getAll(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
    Page<AcademicYear> pageResult = repository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AcademicYearDto getById(Long id) {
    return repository
        .findById(id)
        .map(mapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public AcademicYear getEntityById(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));
  }

  public AcademicYearDto update(Long id, AcademicYearDto request) {
    AcademicYear academicYear = getEntityById(id);
    validateAcademicYear(request, id);
    mapper.updateEntity(request, academicYear);
    AcademicYear saved = repository.save(academicYear);
    return mapper.toResponse(saved);
  }

  @Transactional
  public void activateYear(Long idToActivate) {
    AcademicYear newActiveYear = getEntityById(idToActivate);
    if (Boolean.TRUE.equals(newActiveYear.getIsActive())) {
      return;
    }
    repository
        .findByIsActiveTrue()
        .ifPresent(
            currentActiveYear -> {
              currentActiveYear.setIsActive(false);
            });
    newActiveYear.setIsActive(true);
  }

  public AcademicYearDto getActiveYear() {
    AcademicYear activeEntity =
        repository
            .findByIsActiveTrue()
            .orElseThrow(() -> new ResourceNotFoundException("NO_ACTIVE_ACADEMIC_YEAR_FOUND"));
    return mapper.toResponse(activeEntity);
  }

  public void validateYearExists(Long id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND");
    }
  }
}
