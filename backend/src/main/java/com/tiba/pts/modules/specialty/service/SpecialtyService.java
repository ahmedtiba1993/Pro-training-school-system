package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.dto.request.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.response.SpecialtyResponse;
import com.tiba.pts.modules.specialty.mapper.SpecialtyMapper;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

  private final SpecialtyRepository specialtyRepository;
  private final SpecialtyMapper specialtyMapper;

  @Transactional
  public Long createSpecialty(SpecialtyRequest request) {
    validateSpecialty(null, request);
    Specialty specialty = specialtyMapper.toEntity(request);
    return specialtyRepository.save(specialty).getId();
  }

  @Transactional
  public Long updateSpecialty(Long id, SpecialtyRequest request) {
    Specialty specialty =
        specialtyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));

    // Update basic fields of the specialty
    specialty.setLabel(request.label());
    specialty.setCode(request.code());

    return specialtyRepository.save(specialty).getId();
  }

  private void validateSpecialty(Long specialtyId, SpecialtyRequest request) {
    List<ErrorDetail> erreurs = new ArrayList<>();

    // Check if the specialty name already exists
    boolean nameExists =
        (specialtyId == null)
            ? specialtyRepository.existsByLabelIgnoreCase(request.label())
            : specialtyRepository.existsByLabelIgnoreCaseAndIdNot(request.label(), specialtyId);

    if (nameExists) {
      erreurs.add(new ErrorDetail("name", "SPECIALTY_NAME_ALREADY_EXISTS"));
    }

    // Check if the specialty code already exists
    boolean codeExists =
        (specialtyId == null)
            ? specialtyRepository.existsByCodeIgnoreCase(request.code())
            : specialtyRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), specialtyId);

    if (codeExists) {
      erreurs.add(new ErrorDetail("code", "SPECIALTY_CODE_ALREADY_EXISTS"));
    }

    if (!erreurs.isEmpty()) {
      throw new DuplicateResourceException("VALIDATION_ERREUR", erreurs);
    }
  }

  public List<SpecialtyResponse> getAll() {
    return specialtyRepository.findAll().stream().map(specialtyMapper::toResponse).toList();
  }
}
