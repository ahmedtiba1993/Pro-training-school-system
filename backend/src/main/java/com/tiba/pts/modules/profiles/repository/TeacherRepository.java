package com.tiba.pts.modules.profiles.repository;

import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.profiles.domain.enums.ContractType;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

  boolean existsByCode(String code);

  boolean existsByEmail(String email);

  boolean existsByCin(String cin);

  @EntityGraph(attributePaths = {"specialties"})
  Page<Teacher> findAll(Pageable pageable);

  @Query("SELECT MAX(t.code) FROM Teacher t WHERE t.code LIKE :prefix%")
  String findMaxCodeByPrefix(@Param("prefix") String prefix);

  long countByStatus(TeacherStatus status);

  @Query(
      "SELECT DISTINCT t FROM Teacher t "
          + "LEFT JOIN t.specialties s "
          + "WHERE (:status IS NULL OR t.status = :status) "
          + "AND (:contractType IS NULL OR t.contractType = :contractType) "
          + "AND (:specialtyId IS NULL OR s.id = :specialtyId) "
          + "AND (:keyword IS NULL OR :keyword = '' OR "
          + "LOWER(t.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(t.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(t.cin) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  @EntityGraph(attributePaths = {"specialties"})
  Page<Teacher> searchTeachers(
      @Param("keyword") String keyword,
      @Param("specialtyId") Long specialtyId,
      @Param("contractType") ContractType contractType,
      @Param("status") TeacherStatus status,
      Pageable pageable);

  boolean existsByPhone(String phone);

  // Retrieve teachers with a specific specialty and a specific status
  List<Teacher> findBySpecialtiesIdAndStatus(Long specialtyId, TeacherStatus status);
}
