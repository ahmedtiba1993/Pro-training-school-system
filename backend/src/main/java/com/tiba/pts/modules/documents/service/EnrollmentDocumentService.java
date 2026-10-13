package com.tiba.pts.modules.documents.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.service.PdfGeneratorService;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.classmanagement.repository.ClassGroupRepository;
import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.documents.dto.request.EnrollmentDocumentRequest;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentResponse;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentSimpleResponse;
import com.tiba.pts.modules.documents.mapper.EnrollmentDocumentMapper;
import com.tiba.pts.modules.documents.repository.EnrollmentDocumentRepository;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnrollmentDocumentService {

  private final EnrollmentDocumentRepository documentRepository;
  private final EnrollmentDocumentMapper mapper;
  private final LevelRepository levelRepository;
  private final PdfGeneratorService pdfGeneratorService;
  private final AcademicYearRepository academicYearRepository;
  private final ClassGroupRepository classGroupRepository;

  @Transactional
  public Long addDocument(EnrollmentDocumentRequest request) {

    if (documentRepository.existsByLabelIgnoreCase(request.label())) {
      throw new EntityAlreadyExistsException("DOCUMENTS_ENROLLMENT_DOCUMENT_LABEL_ALREADY_EXISTS");
    }

    if (documentRepository.existsByCodeIgnoreCase(request.code())) {
      throw new EntityAlreadyExistsException("DOCUMENTS_ENROLLMENT_DOCUMENT_CODE_ALREADY_EXISTS");
    }

    EnrollmentDocument document = mapper.toEntity(request);
    document.setIsActive(false);

    if (request.levelIds() != null && !request.levelIds().isEmpty()) {
      List<Level> levels = levelRepository.findAllById(request.levelIds());

      if (levels.size() != request.levelIds().size()) {
        throw new ResourceNotFoundException("DOCUMENTS_LEVEL_NOT_FOUND");
      }

      document.setLevels(new HashSet<>(levels));
    }

    return documentRepository.save(document).getId();
  }

  @Transactional(readOnly = true)
  public List<EnrollmentDocumentResponse> getAll() {
    List<EnrollmentDocument> documents = documentRepository.findAllWithLevels();
    return documents.stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<EnrollmentDocumentResponse> getAllActive() {
    List<EnrollmentDocument> documents = documentRepository.findAllActiveWithLevels();
    return documents.stream().map(mapper::toResponse).toList();
  }

  @Transactional
  public Long updateStatus(Long id, boolean isActive) {
    EnrollmentDocument document =
        documentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("DOCUMENTS_ENROLLMENT_DOCUMENT_NOT_FOUND"));

    document.setIsActive(isActive);
    return documentRepository.save(document).getId();
  }

  @Transactional
  public Long update(Long id, EnrollmentDocumentRequest request) {

    EnrollmentDocument document =
        documentRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("DOCUMENTS_ENROLLMENT_DOCUMENT_NOT_FOUND"));

    if (!document.getLabel().equalsIgnoreCase(request.label())
        && documentRepository.existsByLabelIgnoreCase(request.label())) {
      throw new EntityAlreadyExistsException("DOCUMENTS_ENROLLMENT_DOCUMENT_LABEL_ALREADY_EXISTS");
    }

    mapper.updateEntityFromRequest(request, document);

    if (request.levelIds() != null && !request.levelIds().isEmpty()) {
      List<Level> levels = levelRepository.findAllById(request.levelIds());

      if (levels.size() != request.levelIds().size()) {
        throw new ResourceNotFoundException("DOCUMENTS_LEVEL_NOT_FOUND");
      }

      document.setLevels(new HashSet<>(levels));
    } else {
      document.getLevels().clear();
    }

    EnrollmentDocument updatedDocument = documentRepository.save(document);
    return updatedDocument.getId();
  }

  @Transactional(readOnly = true)
  public EnrollmentDocument getRegistrationDocumentById(Long documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("DOCUMENTS_ENROLLMENT_DOCUMENT_NOT_FOUND"));
  }

  @Transactional(readOnly = true)
  public List<EnrollmentDocumentResponse> getDocumentsByLevelId(Long levelId) {
    if (!levelRepository.existsById(levelId)) {
      throw new ResourceNotFoundException("DOCUMENTS_LEVEL_NOT_FOUND");
    }

    List<EnrollmentDocument> documents = documentRepository.findByLevelsIdAndIsActiveTrue(levelId);
    return documents.stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public byte[] exportArabicEnrollmentForm(Long levelId) {
    List<EnrollmentDocumentResponse> documents;
    if (levelId != null) {
      if (!levelRepository.existsById(levelId)) {
        throw new ResourceNotFoundException("DOCUMENTS_LEVEL_NOT_FOUND");
      }
      documents =
          documentRepository.findByLevelsIdAndIsActiveTrue(levelId).stream()
              .map(mapper::toResponse)
              .toList();
    } else {
      documents = this.getAllActive();
    }

    String currentAcademicYear =
        academicYearRepository.findByIsDefaultTrue().map(AcademicYear::getLabel).orElse(null);

    Map<String, Object> data = new HashMap<>();
    data.put("documents", documents);
    data.put("academicYear", currentAcademicYear);

    return pdfGeneratorService.generateArPdf("enrollment-documents-ar", data);
  }

  @Transactional(readOnly = true)
  public List<EnrollmentDocumentSimpleResponse> getDocumentsByClassGroupId(Long classGroupId) {
    if (!classGroupRepository.existsById(classGroupId)) {
      throw new ResourceNotFoundException("DOCUMENTS_CLASS_GROUP_NOT_FOUND");
    }

    List<EnrollmentDocument> documents = documentRepository.findByClassGroupId(classGroupId);
    return documents.stream().map(mapper::toSimpleResponse).toList();
  }
}
