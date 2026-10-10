package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.dto.request.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.response.SpecialtyResponse;
import com.tiba.pts.modules.specialty.mapper.SpecialtyMapper;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

  private final SpecialtyRepository specialtyRepository;
  private final SpecialtyMapper specialtyMapper;

  @Transactional
  public Long createSpecialty(SpecialtyRequest request) {
    validateSpecialtyUniqueness(null, request);
    Specialty specialty = specialtyMapper.toEntity(request);
    specialty.setIsActive(true);
    return specialtyRepository.save(specialty).getId();
  }

  @Transactional
  public Long updateSpecialty(Long id, SpecialtyRequest request) {
    Specialty specialty =
        specialtyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));

    validateSpecialtyUniqueness(id, request);
    specialtyMapper.updateEntityFromRequest(request, specialty);

    return specialtyRepository.save(specialty).getId();
  }

  @Transactional(readOnly = true)
  public SpecialtyResponse getById(Long id) {
    return specialtyRepository
        .findById(id)
        .map(specialtyMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public List<SpecialtyResponse> getAll() {
    return specialtyRepository.findAll().stream().map(specialtyMapper::toResponse).toList();
  }

  @Transactional
  public Long updateStatus(Long id, boolean isActive) {
    Specialty existingSpecialty =
        specialtyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));

    existingSpecialty.setIsActive(isActive);
    return specialtyRepository.save(existingSpecialty).getId();
  }

  @Transactional(readOnly = true)
  public List<SpecialtyResponse> getAllActive() {
    return specialtyRepository.findByIsActiveTrue().stream()
        .map(specialtyMapper::toResponse)
        .toList();
  }

  private void validateSpecialtyUniqueness(Long specialtyId, SpecialtyRequest request) {
    boolean labelExists =
        (specialtyId == null)
            ? specialtyRepository.existsByLabelIgnoreCase(request.label())
            : specialtyRepository.existsByLabelIgnoreCaseAndIdNot(request.label(), specialtyId);

    if (labelExists) {
      throw new EntityAlreadyExistsException("SPECIALTY_LABEL_ALREADY_EXISTS");
    }

    boolean codeExists =
        (specialtyId == null)
            ? specialtyRepository.existsByCodeIgnoreCase(request.code())
            : specialtyRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), specialtyId);

    if (codeExists) {
      throw new EntityAlreadyExistsException("SPECIALTY_CODE_ALREADY_EXISTS");
    }
  }
}
