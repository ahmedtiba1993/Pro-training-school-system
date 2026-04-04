package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.Period;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {
  // Check label uniqueness for a specific academic year
  boolean existsByLabelIgnoreCaseAndAcademicYearId(String label, Long academicYearId);

  // For modification (excludes the current period ID)
  boolean existsByLabelIgnoreCaseAndAcademicYearIdAndIdNot(
      String label, Long academicYearId, Long id);

  // The name "sessions" must match the attribute name in the Period class
  @EntityGraph(attributePaths = {"sessions"})
  List<Period> findByAcademicYearIdOrderByStartDateAsc(Long academicYearId);
}
