package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.repository.PeriodRepository;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.examscheduling.repository.ExamTimetableRepository;
import com.tiba.pts.modules.subject.repository.SubjectRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.entity.AcceleratedPromotion;
import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionSubjectResponse;
import com.tiba.pts.modules.trainingsession.mapper.PromotionSubjectMapper;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromotionSubjectServiceTest {

  @Mock
  private PromotionSubjectRepository promotionSubjectRepository;

  @Mock
  private PromotionRepository promotionRepository;

  @Mock
  private SubjectRepository subjectRepository;

  @Mock
  private PeriodRepository periodRepository;

  @Mock
  private ExamTimetableRepository examTimetableRepository;

  @Mock
  private PromotionSubjectMapper promotionSubjectMapper;

  @InjectMocks
  private PromotionSubjectService promotionSubjectService;

  @Test
  void getSubjectsByExamTimetable_shouldReturnSubjects_whenAccreditedPromotion() {
    // Given
    Long timetableId = 1L;
    Long periodId = 10L;
    Long promotionId = 20L;

    Period period = new Period();
    period.setId(periodId);

    AccreditedPromotion promotion = new AccreditedPromotion();
    promotion.setId(promotionId);

    ClassGroup classGroup = new ClassGroup();
    classGroup.setPromotion(promotion);

    ExamTimetable examTimetable = new ExamTimetable();
    examTimetable.setId(timetableId);
    examTimetable.setPeriod(period);
    examTimetable.setClassGroup(classGroup);

    PromotionSubject ps = new PromotionSubject();
    List<PromotionSubject> psList = List.of(ps);

    PromotionSubjectResponse psResponse = new PromotionSubjectResponse();
    List<PromotionSubjectResponse> expectedResponses = List.of(psResponse);

    when(examTimetableRepository.findById(timetableId)).thenReturn(Optional.of(examTimetable));
    when(promotionSubjectRepository.findByPromotionIdAndAcademicPeriodId(promotionId, periodId)).thenReturn(psList);
    when(promotionSubjectMapper.toResponseList(psList)).thenReturn(expectedResponses);

    // When
    List<PromotionSubjectResponse> actualResponses = promotionSubjectService.getSubjectsByExamTimetable(timetableId);

    // Then
    assertNotNull(actualResponses);
    assertEquals(1, actualResponses.size());
    verify(examTimetableRepository).findById(timetableId);
    verify(promotionSubjectRepository).findByPromotionIdAndAcademicPeriodId(promotionId, periodId);
    verify(promotionSubjectMapper).toResponseList(psList);
  }

  @Test
  void getSubjectsByExamTimetable_shouldReturnSubjects_whenAcceleratedPromotion() {
    // Given
    Long timetableId = 1L;
    Long periodId = 10L;
    Long promotionId = 20L;

    Period period = new Period();
    period.setId(periodId);

    AcceleratedPromotion promotion = new AcceleratedPromotion();
    promotion.setId(promotionId);

    ClassGroup classGroup = new ClassGroup();
    classGroup.setPromotion(promotion);

    ExamTimetable examTimetable = new ExamTimetable();
    examTimetable.setId(timetableId);
    examTimetable.setPeriod(period);
    examTimetable.setClassGroup(classGroup);

    PromotionSubject ps = new PromotionSubject();
    List<PromotionSubject> psList = List.of(ps);

    PromotionSubjectResponse psResponse = new PromotionSubjectResponse();
    List<PromotionSubjectResponse> expectedResponses = List.of(psResponse);

    when(examTimetableRepository.findById(timetableId)).thenReturn(Optional.of(examTimetable));
    when(promotionSubjectRepository.findByPromotionId(promotionId)).thenReturn(psList);
    when(promotionSubjectMapper.toResponseList(psList)).thenReturn(expectedResponses);

    // When
    List<PromotionSubjectResponse> actualResponses = promotionSubjectService.getSubjectsByExamTimetable(timetableId);

    // Then
    assertNotNull(actualResponses);
    assertEquals(1, actualResponses.size());
    verify(examTimetableRepository).findById(timetableId);
    verify(promotionSubjectRepository).findByPromotionId(promotionId);
    verify(promotionSubjectMapper).toResponseList(psList);
  }

  @Test
  void getSubjectsByExamTimetable_shouldThrowException_whenExamTimetableNotFound() {
    // Given
    Long timetableId = 1L;
    when(examTimetableRepository.findById(timetableId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(ResourceNotFoundException.class, () -> {
      promotionSubjectService.getSubjectsByExamTimetable(timetableId);
    });
    verify(examTimetableRepository).findById(timetableId);
  }

  @Test
  void getSubjectsByExamTimetable_shouldThrowException_whenPeriodNotFound() {
    // Given
    Long timetableId = 1L;
    ExamTimetable examTimetable = new ExamTimetable();
    examTimetable.setId(timetableId);
    examTimetable.setPeriod(null);

    when(examTimetableRepository.findById(timetableId)).thenReturn(Optional.of(examTimetable));

    // When & Then
    assertThrows(BusinessValidationException.class, () -> {
      promotionSubjectService.getSubjectsByExamTimetable(timetableId);
    });
    verify(examTimetableRepository).findById(timetableId);
  }
}
