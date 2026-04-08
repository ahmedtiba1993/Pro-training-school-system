package com.tiba.pts.modules.specialty.repository;

import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

  boolean existsByLabelIgnoreCase(String name);

  boolean existsByCodeIgnoreCase(String code);

  // Exclude the current ID when checking for uniqueness.
  boolean existsByLabelIgnoreCaseAndIdNot(String name, Long id);

  // Exclude the current ID when checking for uniqueness.
  boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
