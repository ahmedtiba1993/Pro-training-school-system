package com.tiba.pts.modules.grading.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.examscheduling.repository.ExamTimetableRepository;
import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.domain.enums.AssessmentStatus;
import com.tiba.pts.modules.grading.domain.enums.AssessmentType;
import com.tiba.pts.modules.grading.dto.request.AssessmentRequest;
import com.tiba.pts.modules.grading.dto.response.AssessmentLookupResponse;
import com.tiba.pts.modules.grading.mapper.AssessmentMapper;
import com.tiba.pts.modules.grading.repository.AssessmentRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import com.tiba.pts.modules.trainingsession.repository.PromotionSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

  private final AssessmentRepository assessmentRepository;
  private final ExamTimetableRepository examTimetableRepository;
  private final AssessmentMapper assessmentMapper;
  private final PromotionSubjectRepository promotionSubjectRepository;

  @Transactional
  public Long createAssessment(AssessmentRequest request) {
    PromotionSubject promotionSubject =
        promotionSubjectRepository
            .findById(request.promotionSubjectId())
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_SUBJECT_NOT_FOUND"));

    int weightPercentage = request.weightPercentage();
    if (request.assessmentType() == AssessmentType.RETAKE) {
      weightPercentage = 0;
    }

    // Validation: The sum of weight percentages for the same promotion subject must not exceed 100%
    Integer currentSum =
        assessmentRepository.sumWeightPercentageByPromotionSubjectId(request.promotionSubjectId());
    if (currentSum + weightPercentage > 100) {
      throw new BusinessValidationException("WEIGHT_PERCENTAGE_SUM_EXCEEDS_100");
    }

    Assessment assessment = assessmentMapper.toEntity(request);
    assessment.setPromotionSubject(promotionSubject);
    assessment.setWeightPercentage(weightPercentage);
    assessment.setStatus(AssessmentStatus.PLANNED);

    return assessmentRepository.save(assessment).getId();
  }

  @Transactional(readOnly = true)
  public List<AssessmentLookupResponse> getUnscheduledAssessmentsForTimetable(Long timetableId) {
    ExamTimetable timetable =
        examTimetableRepository
            .findById(timetableId)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_TIMETABLE_NOT_FOUND"));

    Long classId = timetable.getClassGroup().getId();

    return assessmentRepository
        .findUnscheduledAssessmentsForTimetable(classId, timetableId)
        .stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Transactional
  public void startGrading(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() == AssessmentStatus.PLANNED) {
      assessment.setStatus(AssessmentStatus.GRADING);
      assessmentRepository.save(assessment);
    } else if (assessment.getStatus() != AssessmentStatus.GRADING) {
      throw new BusinessValidationException("INVALID_STATUS_FOR_STARTING_GRADING");
    }
  }

  @Transactional
  public void submit(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() == AssessmentStatus.GRADING) {
      assessment.setStatus(AssessmentStatus.SUBMITTED);
      assessmentRepository.save(assessment);
    } else if (assessment.getStatus() != AssessmentStatus.SUBMITTED) {
      throw new BusinessValidationException("INVALID_STATUS_FOR_SUBMISSION");
    }
  }

  @Transactional
  public void lock(Long id) {
    Assessment assessment =
        assessmentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSESSMENT_NOT_FOUND"));

    if (assessment.getStatus() == AssessmentStatus.SUBMITTED) {
      assessment.setStatus(AssessmentStatus.LOCKED);
      assessmentRepository.save(assessment);
    } else if (assessment.getStatus() != AssessmentStatus.LOCKED) {
      throw new BusinessValidationException("INVALID_STATUS_FOR_LOCKING");
    }
  }
}
