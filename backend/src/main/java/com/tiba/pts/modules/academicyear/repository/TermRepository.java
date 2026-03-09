package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

  boolean existsByNameAndAcademicYearId(String name, Long academicYearId);

  boolean existsByNameAndAcademicYearIdAndIdNot(String name, Long academicYearId, Long termId);

  List<Term> findByAcademicYearId(Long academicYearId);

  Optional<Term> findByAcademicYearIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
      Long academicYearId, LocalDate currentDate1, LocalDate currentDate2);

  @Query("SELECT t FROM Term t LEFT JOIN FETCH t.examSessions WHERE t.id = :termI")
  Optional<Term> findByIdWithSessions(Long termId);
}
