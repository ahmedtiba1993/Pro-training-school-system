package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

  boolean existsByLabelIgnoreCase(String label);

  Optional<AcademicYear> findByIsActiveTrue();

  // Checks if the label exists, BUT excludes the ID of the entity currently being modified
  boolean existsByLabelIgnoreCaseAndIdNot(String label, Long id);

  // Retrieves a maximum of 2 years: either the active one, or those with the status passed as
  // a parameter.
  // OrderByStartDateAsc ensures the current year comes first, followed by the planned one.
  List<AcademicYear> findTop2ByIsActiveTrueOrStatusOrderByStartDateAsc(YearStatus status);
}
