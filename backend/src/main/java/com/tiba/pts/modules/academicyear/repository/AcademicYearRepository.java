package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
  boolean existsByLabel(String label);

  boolean existsByLabelAndIdNot(String label, Long id);

  Optional<AcademicYear> findByIsActiveTrue();
}
