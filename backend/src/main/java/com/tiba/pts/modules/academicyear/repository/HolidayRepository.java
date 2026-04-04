package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

  // Prevent adding duplicate for the same year
  boolean existsByTitleIgnoreCaseAndAcademicYearId(String title, Long academicYearId);

  // Retrieve all holidays for an academic year, sorted chronologically
  List<Holiday> findByAcademicYearIdOrderByStartDateAsc(Long academicYearId);

  // FOR MODIFICATION
  // Excludes the ID of the holiday currently being modified
  boolean existsByTitleIgnoreCaseAndAcademicYearIdAndIdNot(
      String title, Long academicYearId, Long id);
}
