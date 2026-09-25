package com.tiba.pts.modules.grading.controller;

import com.tiba.pts.core.exception.GlobalExceptionHandler;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import com.tiba.pts.modules.grading.dto.response.AssessmentResponse;
import com.tiba.pts.modules.grading.service.AssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

  private MockMvc mockMvc;

  @Mock
  private AssessmentService assessmentService;

  @InjectMocks
  private AssessmentController assessmentController;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(assessmentController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void getAssessmentsByPromotionSubject_shouldReturn200AndAllAssessments() throws Exception {
    Long promotionSubjectId = 1L;
    AssessmentResponse responseDto =
        AssessmentResponse.builder()
            .id(10L)
            .title("Devoir 1")
            .assessmentType(AssessmentType.DS)
            .status(AssessmentStatus.PLANNED)
            .totalMarks(20.0)
            .weightPercentage(30)
            .build();

    when(assessmentService.getAssessmentsByPromotionSubject(promotionSubjectId))
        .thenReturn(List.of(responseDto));

    mockMvc
        .perform(
            get("/api/v1/assessments/promotion-subject/{promotionSubjectId}", promotionSubjectId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("ASSESSMENTS_RETRIEVED_SUCCESSFULLY"))
        .andExpect(jsonPath("$.data[0].id").value(10))
        .andExpect(jsonPath("$.data[0].status").value("PLANNED"));

    verify(assessmentService).getAssessmentsByPromotionSubject(promotionSubjectId);
  }

  @Test
  void getAssessmentsByPromotionSubject_shouldReturn404_whenNotFound() throws Exception {
    Long promotionSubjectId = 999L;
    when(assessmentService.getAssessmentsByPromotionSubject(promotionSubjectId))
        .thenThrow(new ResourceNotFoundException("PROMOTION_SUBJECT_NOT_FOUND"));

    mockMvc
        .perform(
            get("/api/v1/assessments/promotion-subject/{promotionSubjectId}", promotionSubjectId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("PROMOTION_SUBJECT_NOT_FOUND"))
        .andExpect(jsonPath("$.errorCode").value("ERR_NOT_FOUND"));

    verify(assessmentService).getAssessmentsByPromotionSubject(promotionSubjectId);
  }

  @Test
  void getGradingAssessmentsByPromotionSubject_shouldReturn200AndGradingAssessments() throws Exception {
    Long promotionSubjectId = 1L;
    AssessmentResponse responseDto =
        AssessmentResponse.builder()
            .id(20L)
            .title("Examen Final")
            .assessmentType(AssessmentType.FINAL_EXAM)
            .status(AssessmentStatus.GRADING_IN_PROGRESS)
            .totalMarks(20.0)
            .weightPercentage(40)
            .build();

    when(assessmentService.getGradingAssessmentsByPromotionSubject(promotionSubjectId))
        .thenReturn(List.of(responseDto));

    mockMvc
        .perform(
            get("/api/v1/assessments/promotion-subject/{promotionSubjectId}/grading", promotionSubjectId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("ASSESSMENTS_RETRIEVED_SUCCESSFULLY"))
        .andExpect(jsonPath("$.data[0].id").value(20))
        .andExpect(jsonPath("$.data[0].status").value("GRADING_IN_PROGRESS"));

    verify(assessmentService).getGradingAssessmentsByPromotionSubject(promotionSubjectId);
  }

  @Test
  void getGradingAssessmentsByPromotionSubject_shouldReturn404_whenNotFound() throws Exception {
    Long promotionSubjectId = 999L;
    when(assessmentService.getGradingAssessmentsByPromotionSubject(promotionSubjectId))
        .thenThrow(new ResourceNotFoundException("PROMOTION_SUBJECT_NOT_FOUND"));

    mockMvc
        .perform(
            get("/api/v1/assessments/promotion-subject/{promotionSubjectId}/grading", promotionSubjectId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("PROMOTION_SUBJECT_NOT_FOUND"))
        .andExpect(jsonPath("$.errorCode").value("ERR_NOT_FOUND"));

    verify(assessmentService).getGradingAssessmentsByPromotionSubject(promotionSubjectId);
  }
}
