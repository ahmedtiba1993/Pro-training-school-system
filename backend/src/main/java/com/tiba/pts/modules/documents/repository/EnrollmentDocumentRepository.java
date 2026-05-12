package com.tiba.pts.modules.documents.repository;

import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentDocumentRepository extends JpaRepository<EnrollmentDocument, Long> {
  boolean existsByLabelIgnoreCase(String label);

  List<EnrollmentDocument> findByLevelsId(Long levelId);

  boolean existsByCodeIgnoreCase(String code);
}
