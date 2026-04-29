package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
  boolean existsByCin(String cin);

  Optional<Student> findTopByStudentCodeStartingWithOrderByStudentCodeDesc(String prefix);
}
