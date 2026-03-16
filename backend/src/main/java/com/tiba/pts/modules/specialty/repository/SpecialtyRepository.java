package com.tiba.pts.modules.specialty.repository;

import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

  boolean existsByNameIgnoreCase(String name);

  boolean existsByCodeIgnoreCase(String code);

  @Query("SELECT s FROM Specialty s LEFT JOIN FETCH s.associatedLevels")
  List<Specialty> findAllWithLevels();

  // Exclude the current ID when checking for uniqueness.
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  // Exclude the current ID when checking for uniqueness.
  boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

  @Query(
      value =
          "SELECT COUNT(*) > 0 FROM specialty_levels WHERE specialty_id = :specialtyId AND level_id = :levelId",
      nativeQuery = true)
  boolean existsSpecialtyLevelAssociation(
      @Param("specialtyId") Long specialtyId, @Param("levelId") Long levelId);

  List<Specialty> findByAssociatedLevels_Id(Long levelId);
}
