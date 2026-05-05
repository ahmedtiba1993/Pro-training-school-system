package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

  boolean existsByLabelIgnoreCase(String label);

  // Checks if the label exists, BUT excludes the ID of the entity currently being modified
  boolean existsByLabelIgnoreCaseAndIdNot(String label, Long id);

  boolean existsByStatus(YearStatus status);

  Optional<AcademicYear> findByStatus(YearStatus status);

  // Overlap check (handles create and update via :id)
  @Query(
      "SELECT COUNT(a) > 0 FROM AcademicYear a WHERE "
          + "a.startDate <= :endDate "
          + "AND a.endDate >= :startDate "
          + "AND (:id IS NULL OR a.id != :id)")
  boolean existsOverlappingDates(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("id") Long id);

  // Search for the year currently set as default
  Optional<AcademicYear> findByIsDefaultTrue();

  // Find all years whose status is NOT in the provided list
  List<AcademicYear> findByStatusNotInOrderByStartDateDesc(List<YearStatus> statuses);
}
