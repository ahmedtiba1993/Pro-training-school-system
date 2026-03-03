package com.tiba.pts.modules.academicyear.repository;

import com.tiba.pts.modules.academicyear.domain.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

  boolean existsByNameAndAcademicYearId(String name, Long academicYearId);

  boolean existsByNameAndAcademicYearIdAndIdNot(String name, Long academicYearId, Long termId);

  List<Term> findByAcademicYearId(Long academicYearId);
}
