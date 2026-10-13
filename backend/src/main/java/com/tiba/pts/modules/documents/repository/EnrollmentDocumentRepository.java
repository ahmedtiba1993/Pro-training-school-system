package com.tiba.pts.modules.documents.repository;

import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentDocumentRepository extends JpaRepository<EnrollmentDocument, Long> {

  boolean existsByLabelIgnoreCase(String label);

  boolean existsByCodeIgnoreCase(String code);

  @EntityGraph(attributePaths = {"levels"})
  @Query("SELECT d FROM EnrollmentDocument d")
  List<EnrollmentDocument> findAllWithLevels();

  @EntityGraph(attributePaths = {"levels"})
  List<EnrollmentDocument> findByLevelsId(Long levelId);

  @EntityGraph(attributePaths = {"levels"})
  @Query("SELECT d FROM EnrollmentDocument d WHERE d.isActive = true")
  List<EnrollmentDocument> findAllActiveWithLevels();

  @EntityGraph(attributePaths = {"levels"})
  List<EnrollmentDocument> findByLevelsIdAndIsActiveTrue(Long levelId);

  @Query(
      "SELECT DISTINCT d FROM EnrollmentDocument d JOIN d.levels l WHERE l.id = "
          + "(SELECT cg.promotion.training.level.id FROM ClassGroup cg WHERE cg.id = :classGroupId)")
  List<EnrollmentDocument> findByClassGroupId(@Param("classGroupId") Long classGroupId);
}
