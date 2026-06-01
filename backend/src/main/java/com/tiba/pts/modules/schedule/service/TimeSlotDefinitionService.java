package com.tiba.pts.modules.schedule.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.schedule.domain.entity.TimeSlotDefinition;
import com.tiba.pts.modules.schedule.dto.request.TimeSlotDefinitionRequest;
import com.tiba.pts.modules.schedule.dto.response.TimeSlotDefinitionResponse;
import com.tiba.pts.modules.schedule.mapper.TimeSlotDefinitionMapper;
import com.tiba.pts.modules.schedule.repository.TimeSlotDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeSlotDefinitionService {

  private final TimeSlotDefinitionRepository repository;
  private final TimeSlotDefinitionMapper mapper;

  // List must be sorted by orderIndex
  @Transactional(readOnly = true)
  public List<TimeSlotDefinitionResponse> getAllTimeSlots() {
    return repository.findAllByOrderByOrderIndexAsc().stream().map(mapper::toResponse).toList();
  }

  // CREATE: Create a time slot with strict validations
  public Long createTimeSlot(TimeSlotDefinitionRequest request) {
    // Code uniqueness validation
    if (repository.existsByCode(request.code())) {
      throw new EntityAlreadyExistsException("CODE_ALREADY_EXISTS");
    }

    // Anti-overlap validation
    if (repository.existsByOverlap(request.startTime(), request.endTime())) {
      throw new BusinessValidationException("TIME_SLOT_OVERLAP_DETECTED");
    }

    TimeSlotDefinition entity = mapper.toEntity(request);
    return repository.save(entity).getId();
  }

  // UPDATE: Update a time slot
  public Long updateTimeSlot(Long id, TimeSlotDefinitionRequest request) {
    TimeSlotDefinition existingSlot =
        repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TIME_SLOT_NOT_FOUND"));

    // If the code changes, check uniqueness against other IDs
    if (!existingSlot.getCode().equals(request.code())
        && repository.existsByCodeAndIdNot(request.code(), id)) {
      throw new EntityAlreadyExistsException("CODE_ALREADY_EXISTS");
    }

    // If times change, check anti-overlap (excluding the current ID)
    if (!existingSlot.getStartTime().equals(request.startTime())
        || !existingSlot.getEndTime().equals(request.endTime())) {
      if (repository.existsByOverlapExcludingId(id, request.startTime(), request.endTime())) {
        throw new BusinessValidationException("TIME_SLOT_OVERLAP_DETECTED");
      }
    }

    mapper.updateEntityFromRequest(request, existingSlot);
    return repository.save(existingSlot).getId();
  }
}
