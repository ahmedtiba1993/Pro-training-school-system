package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.domain.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  Page<Student> findAll(Pageable pageable);

  Long countByStatus(StudentStatus studentStatus);

  @Query(
      "SELECT s FROM Student s WHERE "
          + "(:status IS NULL OR s.status = :status) AND "
          + "(:keyword IS NULL OR :keyword = '' OR "
          + "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "s.phone LIKE CONCAT('%', :keyword, '%') OR "
          + "s.cin LIKE CONCAT('%', :keyword, '%'))")
  Page<Student> findAllWithFilters(
      @Param("keyword") String keyword, @Param("status") StudentStatus status, Pageable pageable);
}
