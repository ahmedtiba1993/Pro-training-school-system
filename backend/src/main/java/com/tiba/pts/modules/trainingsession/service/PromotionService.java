package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionLookupResponse;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionStatisticsResponse;
import com.tiba.pts.modules.trainingsession.mapper.PromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

  private final PromotionRepository promotionRepository;
  private final TrainingRepository trainingRepository;
  private final PromotionMapper promotionMapper;

  @Transactional(readOnly = true)
  public PromotionStatisticsResponse getStatistics(TrainingType trainingType) {
    return PromotionStatisticsResponse.builder()
        .activeSessionsCount(
            promotionRepository.countByStatusAndTraining_TrainingType(
                PromotionStatus.IN_PROGRESS, trainingType))
        .plannedSessionsCount(
            promotionRepository.countByStatusAndTraining_TrainingType(
                PromotionStatus.PLANNED, trainingType))
        .closedSessionsCount(
            promotionRepository.countByStatusAndTraining_TrainingType(
                PromotionStatus.CLOSED, trainingType))
        .activeLearnersCount(0L)
        .build();
  }

  @Transactional(readOnly = true)
  public List<PromotionLookupResponse> getOpenPromotionsLookupByTrainingId(Long trainingId) {
    if (!trainingRepository.existsById(trainingId)) {
      throw new ResourceNotFoundException("TRAINING_NOT_FOUND");
    }

    List<Promotion> promotions =
        promotionRepository.findByTrainingIdAndStatus(trainingId, PromotionStatus.ENROLLMENT_OPEN);

    return promotions.stream().map(promotionMapper::toLookupResponse).toList();
  }
}
