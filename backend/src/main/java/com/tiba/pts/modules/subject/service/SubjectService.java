package com.tiba.pts.modules.subject.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.service.FileStorageService;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.subject.domain.enums.SubjectStatus;
import com.tiba.pts.modules.subject.dto.request.SubjectRequest;
import com.tiba.pts.modules.subject.dto.response.SubjectResponse;
import com.tiba.pts.modules.subject.dto.response.SubjectShortResponse;
import com.tiba.pts.modules.subject.mapper.SubjectMapper;
import com.tiba.pts.modules.subject.repository.SubjectRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

  private final SubjectRepository subjectRepository;
  private final PromotionRepository promotionRepository;
  private final SubjectMapper subjectMapper;
  private final FileStorageService fileStorageService;
  private final TrainingRepository trainingRepository;

  @Value("${app.storage.subjects-dir}")
  private String subjectsDir;

  @Transactional
  public Long createSubject(SubjectRequest request) {
    if (subjectRepository.existsByCode(request.getCode())) {
      throw new EntityAlreadyExistsException("SUBJECT_CODE_ALREADY_EXISTS");
    }

    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    Subject subject = subjectMapper.toEntity(request);
    subject.setTraining(training);
    subject.setStatus(SubjectStatus.DRAFT);

    Subject savedSubject = subjectRepository.save(subject);
    return subjectMapper.toResponse(savedSubject).getId();
  }

  @Transactional(readOnly = true)
  public SubjectResponse getSubjectById(Long id) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SUBJECT_NOT_FOUND"));
    return subjectMapper.toResponse(subject);
  }

  @Transactional(readOnly = true)
  public List<SubjectResponse> getAllSubjects() {
    return subjectRepository.findAll().stream().map(subjectMapper::toResponse).toList();
  }

  @Transactional
  public Long updateSubject(Long id, SubjectRequest request) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SUBJECT_NOT_FOUND"));

    // Prohibition to modify the code
    if (request.getCode() != null
        && !subject.getCode().equalsIgnoreCase(request.getCode().trim())) {
      throw new BusinessValidationException("SUBJECT_CODE_CANNOT_BE_MODIFIED");
    }

    // Prohibition to modify the attached specialty
    if (request.getTrainingId() != null
        && !subject.getTraining().getId().equals(request.getTrainingId())) {
      throw new BusinessValidationException("TRANING_CANNOT_BE_MODIFIED");
    }

    // Update only the allowed fields via the mapper
    subjectMapper.updateEntityFromRequest(request, subject);

    // Save
    Subject updatedSubject = subjectRepository.save(subject);
    return updatedSubject.getId();
  }

  @Transactional
  public void changeStatus(Long id, SubjectStatus newStatus) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SUBJECT_NOT_FOUND"));

    // Prohibition to set back to DRAFT once published or archived
    if (newStatus == SubjectStatus.DRAFT) {
      throw new BusinessValidationException("CANNOT_SET_STATUS_BACK_TO_DRAFT");
    }

    subject.setStatus(newStatus);
    subjectRepository.save(subject);
  }

  @Transactional
  public Long uploadPdf(Long id, MultipartFile file) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SUBJECT_NOT_FOUND"));

    if (file.getContentType() == null
        || !file.getContentType().equalsIgnoreCase("application/pdf")) {
      throw new BusinessValidationException("ONLY_PDF_FILES_ALLOWED");
    }

    // Cleanup only if the old file didn't already have the current code name
    // (because if it's the same code, the storeFile function will overwrite it cleanly)
    if (subject.getPdfFilePath() != null && !subject.getPdfFilePath().contains(subject.getCode())) {
      fileStorageService.deleteFile(subject.getPdfFilePath());
    }

    // Extraction of the subject code (e.g.: "MATH-101")
    String customFilename = subject.getCode();

    // CALL TO THE NEW FUNCTION (3 parameters): file, directory, customName
    String filePath = fileStorageService.storeFile(file, subjectsDir, customFilename);

    subject.setPdfFilePath(filePath);
    subjectRepository.save(subject);

    return subject.getId();
  }

  @Transactional(readOnly = true)
  public Resource getPdfAsResource(Long id) {
    Subject subject =
        subjectRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SUBJECT_NOT_FOUND"));

    if (subject.getPdfFilePath() == null || subject.getPdfFilePath().trim().isEmpty()) {
      throw new ResourceNotFoundException("SUBJECT_PDF_NOT_FOUND");
    }

    // We use the generic method from your FileStorageService
    return fileStorageService.loadFileAsResource(subject.getPdfFilePath());
  }

  /**
   * Retrieves all subjects from the Pedagogical Catalogue eligible for a given promotion in short
   * format (Record).
   */
  public List<SubjectShortResponse> getCatalogSubjectsShortByPromotion(Long promotionId) {
    // Verification and loading of the promotion
    Promotion promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(() -> new EntityNotFoundException("PROMOTION_NOT_FOUND"));

    // Retrieval of ACTIVE subjects from the corresponding training
    List<Subject> subjects =
        subjectRepository.findByTrainingIdAndStatus(
            promotion.getTraining().getId(), SubjectStatus.ACTIVE);

    // Mapping to the list of records
    return subjectMapper.toShortResponseList(subjects);
  }
}
