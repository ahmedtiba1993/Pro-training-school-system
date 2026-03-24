package com.tiba.pts.modules.person.repository;

import com.tiba.pts.modules.person.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
  boolean existsByEmail(String email);

  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsByCin(String cin);
}
