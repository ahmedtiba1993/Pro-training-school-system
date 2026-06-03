package com.tiba.pts.modules.enrollment.repository;

import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.response.UnassignedEnrollmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  Optional<Enrollment> findTopByEnrollmentNumberStartingWithOrderByEnrollmentNumberDesc(
      String prefix);

  boolean existsByStudentIdAndPromotionId(Long id, Long id1);

  /** Paged multi-criteria filter with EntityGraph to optimize performance (avoids N+1 selects). */
  @EntityGraph(
      attributePaths = {
        "student",
        "promotion",
        "promotion.training",
        "promotion.training.level",
        "promotion.training.specialty"
      })
  @Query(
      """
      SELECT e FROM Enrollment e
      WHERE (:keyword IS NULL OR
             LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(e.student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(e.enrollmentNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:levelId IS NULL OR e.promotion.training.level.id = :levelId)
      AND (:specialtyId IS NULL OR e.promotion.training.specialty.id = :specialtyId)
      AND (:promotionId IS NULL OR e.promotion.id = :promotionId)
      AND (:status IS NULL OR e.status = :status)
      """)
  Page<Enrollment> findAllWithFilters(
      @Param("keyword") String keyword,
      @Param("levelId") Long levelId,
      @Param("specialtyId") Long specialtyId,
      @Param("promotionId") Long promotionId,
      @Param("status") EnrollmentStatus status,
      Pageable pageable);

  boolean existsByStudentIdAndStatusIn(Long studentId, List<EnrollmentStatus> statuses);

  @Query(
      "SELECT new com.tiba.pts.modules.enrollment.dto.response.UnassignedEnrollmentResponse("
          + "e.id, e.enrollmentNumber, e.student.firstName, e.student.lastName, e.student.studentCode, e.student.birthDate) "
          + "FROM Enrollment e "
          + "WHERE e.promotion.id = :promotionId "
          + "AND e.status IN (com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus.VALIDATED, "
          + "                 com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus.CONDITIONALLY_VALIDATED) "
          + "AND NOT EXISTS ("
          + "  SELECT ca FROM ClassAssignment ca WHERE ca.enrollment.id = e.id"
          + ") "
          + "AND (:firstName IS NULL OR LOWER(e.student.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) "
          + "AND (:lastName IS NULL OR LOWER(e.student.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) "
          + "AND (:cin IS NULL OR e.student.cin LIKE CONCAT('%', :cin, '%')) "
          + "AND (:phone IS NULL OR e.student.phone LIKE CONCAT('%', :phone, '%')) "
          + "AND (:studentCode IS NULL OR LOWER(e.student.studentCode) LIKE LOWER(CONCAT('%', :studentCode, '%')))")
  List<UnassignedEnrollmentResponse> findUnassignedValidatedEnrollmentsWithFilters(
      @Param("promotionId") Long promotionId,
      @Param("firstName") String firstName,
      @Param("lastName") String lastName,
      @Param("cin") String cin,
      @Param("phone") String phone,
      @Param("studentCode") String studentCode);

  long countByIdIn(List<Long> ids);

  @Query("""
      SELECT COUNT(e) 
      FROM Enrollment e 
      WHERE e.id IN :ids 
      AND EXISTS (
          SELECT ca 
          FROM ClassAssignment ca 
          WHERE ca.enrollment.id = e.id 
          AND ca.classGroup.id = :classGroupId
      )
  """)
  long countByIdInAndClassGroupId(@Param("ids") List<Long> ids, @Param("classGroupId") Long classGroupId);
}

