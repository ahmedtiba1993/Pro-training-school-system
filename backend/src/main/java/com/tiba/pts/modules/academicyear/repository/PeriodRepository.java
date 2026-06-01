package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.Period;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {

  // Anti-Overlap Rule (Handles both Create and Update via ID check)
  @Query(
      "SELECT COUNT(p) > 0 FROM Period p WHERE p.academicYear.id = :yearId "
          + "AND p.startDate <= :endDate AND p.endDate >= :startDate "
          + "AND (:id IS NULL OR p.id != :id)")
  boolean existsOverlappingDates(
      @Param("yearId") Long yearId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("id") Long id);

  // Label uniqueness (Excludes the current ID for updates)
  boolean existsByAcademicYearIdAndLabelIgnoreCaseAndIdNot(Long yearId, String label, Long id);

  // For creation (no ID to exclude)
  boolean existsByAcademicYearIdAndLabelIgnoreCase(Long yearId, String label);

  // Check label uniqueness for a specific academic year
  boolean existsByLabelIgnoreCaseAndAcademicYearId(String label, Long academicYearId);

  // For modification (excludes the current period ID)
  boolean existsByLabelIgnoreCaseAndAcademicYearIdAndIdNot(
      String label, Long academicYearId, Long id);

  // The name "sessions" must match the attribute name in the Period class
  @EntityGraph(attributePaths = {"sessions"})
  List<Period> findByAcademicYearIdOrderByStartDateAsc(Long academicYearId);

  @Query("SELECT MIN(p.startDate) FROM Period p WHERE p.academicYear.id = :yearId")
  Optional<LocalDate> findMinStartDateByAcademicYearId(@Param("yearId") Long yearId);

  @Query("SELECT MAX(p.endDate) FROM Period p WHERE p.academicYear.id = :yearId")
  Optional<LocalDate> findMaxEndDateByAcademicYearId(@Param("yearId") Long yearId);

  // Finds the period of a specific year that encompasses today's date
  @Query(
      "SELECT p FROM Period p WHERE p.academicYear.id = :yearId AND :today BETWEEN p.startDate AND p.endDate")
  Optional<Period> findCurrentPeriodByYearId(
      @Param("yearId") Long yearId, @Param("today") LocalDate today);

  @Query(
      "SELECT p FROM Period p "
          + "WHERE p.academicYear.isDefault = true "
          + "ORDER BY p.startDate ASC")
  List<Period> findAllByAcademicYearIsDefaultTrue();
}
