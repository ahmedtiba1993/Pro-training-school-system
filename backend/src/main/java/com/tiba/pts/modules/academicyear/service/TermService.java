package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Term;
import com.tiba.pts.modules.academicyear.dto.TermDto;
import com.tiba.pts.modules.academicyear.mapper.TermMapper;
import com.tiba.pts.modules.academicyear.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TermService {

  private final TermRepository repository;
  private final TermMapper mapper;
  private final AcademicYearService academicYearService;

  @Transactional
  public Long create(TermDto request) {
    AcademicYear academicYear = academicYearService.getEntityById(request.getAcademicYearId());
    checkBusinessRules(request, null);
    Term entity = mapper.toEntity(request);
    entity.setAcademicYear(academicYear);
    return repository.save(entity).getId();
  }

  @Transactional(readOnly = true)
  public List<TermDto> getAllByAcademicYear(Long academicYearId) {
    academicYearService.validateYearExists(academicYearId);
    List<Term> terms = repository.findByAcademicYearId(academicYearId);
    return terms.stream().map(mapper::toResponse).toList();
  }

  @Transactional
  public TermDto update(Long id, TermDto request) {
    Term existingEntity = getEntityById(id);
    AcademicYear year = academicYearService.getEntityById(request.getAcademicYearId());
    checkBusinessRules(request, id);
    mapper.updateEntity(request, existingEntity);
    existingEntity.setAcademicYear(year);
    Term saved = repository.save(existingEntity);
    return mapper.toResponse(saved);
  }

  private void checkBusinessRules(TermDto request, Long excludeTermId) {
    List<ErrorDetail> errors = new ArrayList<>();
    boolean exists =
        (excludeTermId == null)
            ? repository.existsByNameAndAcademicYearId(
                request.getName(), request.getAcademicYearId())
            : repository.existsByNameAndAcademicYearIdAndIdNot(
                request.getName(), request.getAcademicYearId(), excludeTermId);

    if (exists) {
      errors.add(new ErrorDetail("name", "TERM_NAME_ALREADY_EXISTS_IN_THIS_YEAR"));
    }

    if (!errors.isEmpty()) {
      throw new DuplicateResourceException("BUSINESS_RULES_VIOLATION", errors);
    }
  }

  public Term getEntityById(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TERM_NOT_FOUND"));
  }
}
