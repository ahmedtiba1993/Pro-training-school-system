package com.tiba.pts.modules.dashboard.service;

import com.tiba.pts.modules.dashboard.dto.response.EnrollmentStatsResponse;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.mapper.EnrollmentMapper;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.enrollment.repository.projection.EnrollmentGenderStatsProjection;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

  @Mock
  private PromotionRepository promotionRepository;

  @Mock
  private EnrollmentRepository enrollmentRepository;

  @Mock
  private EnrollmentMapper enrollmentMapper;

  @InjectMocks
  private DashboardService dashboardService;

  @Test
  void getActiveSessionStudentsCount_success() {
    // Given
    long expectedCount = 42L;
    when(promotionRepository.countActiveSessionStudents()).thenReturn(expectedCount);

    // When
    long actualCount = dashboardService.getActiveSessionStudentsCount();

    // Then
    assertEquals(expectedCount, actualCount);
    verify(promotionRepository).countActiveSessionStudents();
  }

  @Test
  void getAdminEnrollmentStats_success() {
    // Given
    List<EnrollmentStatus> statuses = List.of(
        EnrollmentStatus.CONDITIONALLY_VALIDATED,
        EnrollmentStatus.VALIDATED
    );

    Enrollment enrollment1 = new Enrollment();
    Enrollment enrollment2 = new Enrollment();
    List<Enrollment> enrollments = List.of(enrollment1, enrollment2);

    EnrollmentResponse response1 = new EnrollmentResponse();
    EnrollmentResponse response2 = new EnrollmentResponse();

    when(enrollmentRepository.findByStatusIn(statuses)).thenReturn(enrollments);
    when(enrollmentMapper.toResponse(enrollment1)).thenReturn(response1);
    when(enrollmentMapper.toResponse(enrollment2)).thenReturn(response2);

    EnrollmentGenderStatsProjection statsProjection = mock(EnrollmentGenderStatsProjection.class);
    when(statsProjection.getTotalCount()).thenReturn(2L);
    when(statsProjection.getMaleCount()).thenReturn(1L);
    when(statsProjection.getFemaleCount()).thenReturn(1L);

    when(enrollmentRepository.countByStatusInGroupedByGender(statuses)).thenReturn(statsProjection);

    // When
    EnrollmentStatsResponse result = dashboardService.getAdminEnrollmentStats();

    // Then
    assertNotNull(result);
    assertEquals(2, result.getEnrollments().size());
    assertEquals(2L, result.getTotalCount());
    assertEquals(1L, result.getMaleCount());
    assertEquals(1L, result.getFemaleCount());

    verify(enrollmentRepository).findByStatusIn(statuses);
    verify(enrollmentRepository).countByStatusInGroupedByGender(statuses);
  }
}
