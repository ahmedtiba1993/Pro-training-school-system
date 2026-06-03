package com.tiba.pts.modules.classmanagement.repository;

import com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Long> {

  // RULE: The couple [enrollment_id + class_group_id] must be unique
  boolean existsByClassGroupIdAndEnrollmentId(Long classGroupId, Long enrollmentId);

  @Query("SELECT COUNT(ca) FROM ClassAssignment ca WHERE ca.classGroup.status = :status")
  long countByClassGroupStatus(@Param("status") ClassStatus status);

  @EntityGraph(attributePaths = {"enrollment", "enrollment.student"})
  List<ClassAssignment> findByClassGroupId(Long classGroupId);

  @EntityGraph(attributePaths = {"enrollment", "enrollment.student"})
  List<ClassAssignment> findByClassGroupIdAndEnrollmentStatusIn(Long classGroupId, List<EnrollmentStatus> statuses);

  // SENIOR SHIELDING: Full cascade join to avoid LazyLoadingException and N+1
  @EntityGraph(
      attributePaths = {
        "enrollment",
        "enrollment.student",
        "enrollment.enrollmentDocumentSubmissions",
        "enrollment.enrollmentDocumentSubmissions.document"
      })
  List<ClassAssignment> findWithSubmissionsByClassGroupId(Long classGroupId);

  long countByClassGroupId(Long classGroupId);

  long countByClassGroupIdAndEnrollmentStatusIn(Long classGroupId, List<EnrollmentStatus> statuses);
}

