package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
  boolean existsByCin(String cin);

  Optional<Student> findTopByStudentCodeStartingWithOrderByStudentCodeDesc(String prefix);

  @Query(
      "SELECT s FROM Student s WHERE "
          + "(:keyword IS NULL OR :keyword = '' OR "
          + "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.cin) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  List<Student> searchStudentsAsList(@Param("keyword") String keyword);
}
