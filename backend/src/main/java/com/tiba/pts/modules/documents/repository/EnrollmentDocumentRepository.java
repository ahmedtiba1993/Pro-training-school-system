package com.tiba.pts.modules.documents.repository;

import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentDocumentRepository extends JpaRepository<EnrollmentDocument, Long> {
  boolean existsByLabelIgnoreCase(String label);

  List<EnrollmentDocument> findByLevelsId(Long levelId);

  boolean existsByCodeIgnoreCase(String code);

  @Query(
      "SELECT d FROM EnrollmentDocument d JOIN d.levels l WHERE l.id = "
          + "(SELECT cg.promotion.training.level.id FROM ClassGroup cg WHERE cg.id = :classGroupId)")
  List<EnrollmentDocument> findByClassGroupId(@Param("classGroupId") Long classGroupId);
}
