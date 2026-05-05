package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

  // Prevent adding duplicate for the same year
  boolean existsByLabelIgnoreCaseAndAcademicYearId(String label, Long academicYearId);

  // Retrieve all holidays for an academic year, sorted chronologically
  List<Holiday> findByAcademicYearIdOrderByStartDateAsc(Long academicYearId);

  // FOR MODIFICATION
  // Excludes the ID of the holiday currently being modified
  boolean existsByLabelIgnoreCaseAndAcademicYearIdAndIdNot(
      String label, Long academicYearId, Long id);

  // Anti-Overlap Query (Ready for Creation and Update)
  @Query(
      "SELECT COUNT(h) > 0 FROM Holiday h WHERE h.academicYear.id = :yearId "
          + "AND h.startDate <= :endDate AND h.endDate >= :startDate "
          + "AND (:id IS NULL OR h.id != :id)")
  boolean existsOverlappingDates(
      @Param("yearId") Long yearId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("id") Long id);

  @Query("SELECT MIN(h.startDate) FROM Holiday h WHERE h.academicYear.id = :yearId")
  Optional<LocalDate> findMinStartDateByAcademicYearId(@Param("yearId") Long yearId);

  @Query("SELECT MAX(h.endDate) FROM Holiday h WHERE h.academicYear.id = :yearId")
  Optional<LocalDate> findMaxEndDateByAcademicYearId(@Param("yearId") Long yearId);
}
