package com.tiba.pts.modules.documents.repository;

import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentDocumentRepository extends JpaRepository<EnrollmentDocument, Long> {
  boolean existsByNameIgnoreCase(String name);
}
