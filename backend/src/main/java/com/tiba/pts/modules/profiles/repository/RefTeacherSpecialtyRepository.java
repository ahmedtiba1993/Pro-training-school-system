package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.RefTeacherSpecialty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RefTeacherSpecialtyRepository extends JpaRepository<RefTeacherSpecialty, Long> {

  Set<RefTeacherSpecialty> findByIdIn(Set<Long> ids);

  boolean existsByCode(String code);

  boolean existsByLabel(String label);

  boolean existsByLabelAndIdNot(String label, Long id);

  @EntityGraph(attributePaths = {"teachers"})
  List<RefTeacherSpecialty> findAll();

  @EntityGraph(attributePaths = {"teachers"})
  List<RefTeacherSpecialty> findByLabelContainingIgnoreCase(String keyword);
}
