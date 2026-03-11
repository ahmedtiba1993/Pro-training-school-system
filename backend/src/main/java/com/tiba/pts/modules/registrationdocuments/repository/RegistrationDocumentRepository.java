package com.tiba.pts.modules.registrationdocuments.repository;

import com.tiba.pts.modules.registrationdocuments.domain.entity.RegistrationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationDocumentRepository extends JpaRepository<RegistrationDocument, Long> {
  boolean existsByNameIgnoreCase(String name);
}
