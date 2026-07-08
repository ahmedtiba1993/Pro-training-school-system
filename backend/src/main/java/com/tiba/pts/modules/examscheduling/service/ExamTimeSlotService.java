package com.tiba.pts.modules.examscheduling.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimeSlot;
import com.tiba.pts.modules.examscheduling.dto.request.ExamTimeSlotRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamTimeSlotResponse;
import com.tiba.pts.modules.examscheduling.mapper.ExamTimeSlotMapper;
import com.tiba.pts.modules.examscheduling.repository.ExamTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamTimeSlotService {

  private final ExamTimeSlotRepository examTimeSlotRepository;
  private final ExamTimeSlotMapper examTimeSlotMapper;

  @Transactional
  public Long createExamTimeSlot(ExamTimeSlotRequest request) {
    if (examTimeSlotRepository.existsByCodeIgnoreCase(request.code())) {
      throw new EntityAlreadyExistsException("EXAM_TIME_SLOT_CODE_ALREADY_EXISTS");
    }

    ExamTimeSlot examTimeSlot = examTimeSlotMapper.toEntity(request);
    ExamTimeSlot savedSlot = examTimeSlotRepository.save(examTimeSlot);
    return savedSlot.getId();
  }

  @Transactional(readOnly = true)
  public List<ExamTimeSlotResponse> getAllExamTimeSlots() {
    return examTimeSlotRepository.findAll().stream()
        .map(examTimeSlotMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ExamTimeSlotResponse getExamTimeSlotById(Long id) {
    return examTimeSlotRepository
        .findById(id)
        .map(examTimeSlotMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIME_SLOT_NOT_FOUND"));
  }

  @Transactional
  public void toggleActiveStatus(Long id) {
    ExamTimeSlot examTimeSlot =
        examTimeSlotRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIME_SLOT_NOT_FOUND"));
    examTimeSlot.setIsActive(!examTimeSlot.getIsActive());
    examTimeSlotRepository.save(examTimeSlot);
  }

  @Transactional(readOnly = true)
  public List<ExamTimeSlotResponse> getActiveExamTimeSlots() {
    return examTimeSlotRepository.findByIsActiveTrue().stream()
        .map(examTimeSlotMapper::toResponse)
        .toList();
  }
}
