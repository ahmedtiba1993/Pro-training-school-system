package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionStatisticsResponse;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {

  private final PromotionRepository promotionRepository;

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
}
