package com.tiba.pts.modules.grading.service;

import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.examscheduling.repository.ExamTimetableRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import com.tiba.pts.modules.grading.dto.response.AssessmentResponse;
import com.tiba.pts.modules.grading.mapper.AssessmentMapper;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

  @Mock
  private AssessmentRepository assessmentRepository;

  @Mock
  private ExamTimetableRepository examTimetableRepository;

  @Mock
  private AssessmentMapper assessmentMapper;

  @Mock
  private PromotionSubjectRepository promotionSubjectRepository;

  @InjectMocks
  private AssessmentService assessmentService;

  @Test
  void getAssessmentsByPromotionSubject_shouldReturnAllAssessments_whenPromotionSubjectExists() {
    // Given
    Long promotionSubjectId = 10L;
    when(promotionSubjectRepository.existsById(promotionSubjectId)).thenReturn(true);

    Assessment assessment1 =
        Assessment.builder()
            .id(1L)
            .title("Devoir 1")
            .assessmentType(AssessmentType.DS)
            .status(AssessmentStatus.PLANNED)
            .build();
    Assessment assessment2 =
        Assessment.builder()
            .id(2L)
            .title("Examen Final")
            .assessmentType(AssessmentType.FINAL_EXAM)
            .status(AssessmentStatus.GRADING_IN_PROGRESS)
            .build();

    AssessmentResponse response1 =
        AssessmentResponse.builder().id(1L).title("Devoir 1").status(AssessmentStatus.PLANNED).build();
    AssessmentResponse response2 =
        AssessmentResponse.builder().id(2L).title("Examen Final").status(AssessmentStatus.GRADING_IN_PROGRESS).build();

    when(assessmentRepository.findByPromotionSubjectId(promotionSubjectId))
        .thenReturn(List.of(assessment1, assessment2));
    when(assessmentMapper.toResponse(assessment1)).thenReturn(response1);
    when(assessmentMapper.toResponse(assessment2)).thenReturn(response2);

    // When
    List<AssessmentResponse> results =
        assessmentService.getAssessmentsByPromotionSubject(promotionSubjectId);

    // Then
    assertNotNull(results);
    assertEquals(2, results.size());
    verify(promotionSubjectRepository).existsById(promotionSubjectId);
    verify(assessmentRepository).findByPromotionSubjectId(promotionSubjectId);
    verify(assessmentMapper).toResponse(assessment1);
    verify(assessmentMapper).toResponse(assessment2);
  }

  @Test
  void getAssessmentsByPromotionSubject_shouldThrowException_whenPromotionSubjectNotFound() {
    // Given
    Long promotionSubjectId = 999L;
    when(promotionSubjectRepository.existsById(promotionSubjectId)).thenReturn(false);

    // When & Then
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> assessmentService.getAssessmentsByPromotionSubject(promotionSubjectId));

    assertEquals("PROMOTION_SUBJECT_NOT_FOUND", exception.getMessage());
    verify(promotionSubjectRepository).existsById(promotionSubjectId);
    verifyNoInteractions(assessmentRepository);
  }

  @Test
  void getGradingAssessmentsByPromotionSubject_shouldReturnOnlyGradingInProgressAssessments() {
    // Given
    Long promotionSubjectId = 10L;
    when(promotionSubjectRepository.existsById(promotionSubjectId)).thenReturn(true);

    Assessment assessment =
        Assessment.builder()
            .id(2L)
            .title("Examen Final")
            .assessmentType(AssessmentType.FINAL_EXAM)
            .status(AssessmentStatus.GRADING_IN_PROGRESS)
            .build();

    AssessmentResponse response =
        AssessmentResponse.builder()
            .id(2L)
            .title("Examen Final")
            .assessmentType(AssessmentType.FINAL_EXAM)
            .status(AssessmentStatus.GRADING_IN_PROGRESS)
            .build();

    when(assessmentRepository.findByPromotionSubjectIdAndStatus(
            promotionSubjectId, AssessmentStatus.GRADING_IN_PROGRESS))
        .thenReturn(List.of(assessment));
    when(assessmentMapper.toResponse(assessment)).thenReturn(response);

    // When
    List<AssessmentResponse> results =
        assessmentService.getGradingAssessmentsByPromotionSubject(promotionSubjectId);

    // Then
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(AssessmentStatus.GRADING_IN_PROGRESS, results.getFirst().getStatus());
    verify(promotionSubjectRepository).existsById(promotionSubjectId);
    verify(assessmentRepository)
        .findByPromotionSubjectIdAndStatus(promotionSubjectId, AssessmentStatus.GRADING_IN_PROGRESS);
    verify(assessmentMapper).toResponse(assessment);
  }

  @Test
  void getGradingAssessmentsByPromotionSubject_shouldThrowException_whenPromotionSubjectNotFound() {
    // Given
    Long promotionSubjectId = 999L;
    when(promotionSubjectRepository.existsById(promotionSubjectId)).thenReturn(false);

    // When & Then
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> assessmentService.getGradingAssessmentsByPromotionSubject(promotionSubjectId));

    assertEquals("PROMOTION_SUBJECT_NOT_FOUND", exception.getMessage());
    verify(promotionSubjectRepository).existsById(promotionSubjectId);
    verifyNoInteractions(assessmentRepository);
  }
}
