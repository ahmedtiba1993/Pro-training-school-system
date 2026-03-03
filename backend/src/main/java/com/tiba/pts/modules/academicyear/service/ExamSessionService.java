package com.tiba.pts.modules.academicyear.service;

import com.tiba.pts.core.dto.ErrorDetail;
import com.tiba.pts.core.exception.DuplicateResourceException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.entity.Term;
import com.tiba.pts.modules.academicyear.dto.ExamSessionDto;
import com.tiba.pts.modules.academicyear.mapper.ExamSessionMapper;
import com.tiba.pts.modules.academicyear.repository.ExamSessionRepository;
import com.tiba.pts.modules.academicyear.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSessionService {

  private final ExamSessionRepository repository;
  private final ExamSessionMapper mapper;
  private final TermService termService;
  private final TermRepository termRepository;

  @Transactional
  public Long create(ExamSessionDto request) {
    Term term = termService.getEntityById(request.getTermId());
    checkBusinessRules(request, term, null);
    ExamSession entity = mapper.toEntity(request);
    entity.setTerm(term);
    return repository.save(entity).getId();
  }

  public List<ExamSessionDto> getAllByTerm(Long termId) {
    if (!termRepository.existsById(termId)) {
      throw new ResourceNotFoundException("TERM_NOT_FOUND");
    }
    List<ExamSession> sessions = repository.findByTermId(termId);
    return sessions.stream().map(mapper::toResponse).collect(Collectors.toList());
  }

  private void checkBusinessRules(ExamSessionDto request, Term term, Long excludeId) {
    List<ErrorDetail> errors = new ArrayList<>();

    boolean isDuplicate =
        (excludeId == null)
            ? repository.existsByTermIdAndSessionType(term.getId(), request.getSessionType())
            : repository.existsByTermIdAndSessionTypeAndIdNot(
                term.getId(), request.getSessionType(), excludeId);

    if (isDuplicate) {
      errors.add(new ErrorDetail("sessionType", "SESSION_TYPE_ALREADY_EXISTS_FOR_THIS_TERM"));
    }
    if (!errors.isEmpty()) {
      throw new DuplicateResourceException("BUSINESS_RULES_VIOLATION", errors);
    }
  }
}
