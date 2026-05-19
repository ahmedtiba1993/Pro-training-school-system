package com.tiba.pts.modules.subject.repository;

import com.tiba.pts.modules.subject.domain.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
  boolean existsByCode(String code);

  Optional<Subject> findByCode(String code);
}
