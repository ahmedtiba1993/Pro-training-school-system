package com.tiba.pts.modules.dashboard.service;

import com.tiba.pts.modules.dashboard.dto.response.EnrollmentStatsResponse;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.mapper.EnrollmentMapper;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.enrollment.repository.projection.EnrollmentGenderStatsProjection;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final PromotionRepository promotionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final EnrollmentMapper enrollmentMapper;

  @Transactional(readOnly = true)
  public long getActiveSessionStudentsCount() {
    return promotionRepository.countActiveSessionStudents();
  }

  @Transactional(readOnly = true)
  public EnrollmentStatsResponse getAdminEnrollmentStats() {
    List<EnrollmentStatus> statuses =
        List.of(EnrollmentStatus.CONDITIONALLY_VALIDATED, EnrollmentStatus.VALIDATED);

    EnrollmentGenderStatsProjection statsProjection =
        enrollmentRepository.countByStatusInGroupedByGender(statuses);

    long totalCount = 0;
    long maleCount = 0;
    long femaleCount = 0;

    if (statsProjection != null) {
      totalCount = statsProjection.getTotalCount();
      maleCount = statsProjection.getMaleCount();
      femaleCount = statsProjection.getFemaleCount();
    }

    List<Enrollment> enrollments = enrollmentRepository.findByStatusIn(statuses);
    List<EnrollmentResponse> enrollmentResponses = enrollments.stream()
        .map(enrollmentMapper::toResponse)
        .toList();

    return EnrollmentStatsResponse.builder()
        .totalCount(totalCount)
        .maleCount(maleCount)
        .femaleCount(femaleCount)
        .enrollments(enrollmentResponses)
        .build();
  }
}
