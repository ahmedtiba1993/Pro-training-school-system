package com.tiba.pts.modules.documents.service;

import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.documents.dto.request.EnrollmentDocumentRequest;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentResponse;
import com.tiba.pts.modules.documents.mapper.EnrollmentDocumentMapper;
import com.tiba.pts.modules.documents.repository.EnrollmentDocumentRepository;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentDocumentService {

  private final EnrollmentDocumentRepository documentRepository;
  private final EnrollmentDocumentMapper mapper;
  private final LevelRepository levelRepository;

  @Transactional
  public Long addDocument(EnrollmentDocumentRequest request) {

    if (documentRepository.existsByNameIgnoreCase(request.getName())) {
      throw new ResourceNotFoundException("DOCUMENT_NAME_ALREADY_EXISTS");
    }

    // Convert the Request DTO to Entity
    EnrollmentDocument document = mapper.toEntity(request);

    // Associate Levels if they are provided
    if (request.getLevelIds() != null && !request.getLevelIds().isEmpty()) {
      // Retrieve all corresponding levels in a single query
      List<Level> levels = levelRepository.findAllById(request.getLevelIds());

      // Optional safety check: verify that all requested levels were found
      if (levels.size() != request.getLevelIds().size()) {
        throw new ResourceNotFoundException("Un ou plusieurs niveaux fournis sont introuvables.");
      }

      document.setLevels(new HashSet<>(levels));
    }

    return documentRepository.save(document).getId();
  }

  @Transactional(readOnly = true)
  public List<EnrollmentDocumentResponse> getAll() {
    List<EnrollmentDocument> documents = documentRepository.findAll();
    return documents.stream().map(mapper::toResponse).collect(Collectors.toList());
  }

  @Transactional
  public Long update(Long id, EnrollmentDocumentRequest request) {

    // Find the existing document
    EnrollmentDocument document =
        documentRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND"));

    // Check name uniqueness (only if the name has been modified)
    if (!document.getName().equalsIgnoreCase(request.getName())
        && documentRepository.existsByNameIgnoreCase(request.getName())) {
      throw new ResourceNotFoundException("DOCUMENT_NAME_ALREADY_EXISTS");
    }

    // Update simple properties
    document.setName(request.getName());
    document.setQuantity(request.getQuantity());
    document.setNature(request.getNature());
    document.setCondition(request.getCondition());
    document.setMandatory(request.getMandatory());

    // Update levels
    if (request.getLevelIds() != null && !request.getLevelIds().isEmpty()) {
      List<Level> levels = levelRepository.findAllById(request.getLevelIds());

      if (levels.size() != request.getLevelIds().size()) {
        throw new ResourceNotFoundException("One or more provided levels were not found.");
      }

      // Replace the old list with the new one
      document.setLevels(new HashSet<>(levels));
    }

    // Save the entity and return the response DTO ID
    EnrollmentDocument updatedDocument = documentRepository.save(document);
    return mapper.toResponse(updatedDocument).getId();
  }

  public EnrollmentDocument getRegistrationDocumentById(Long documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND_IN_CATALOG: " + documentId));
  }
}
