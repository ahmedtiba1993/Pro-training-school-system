package com.tiba.pts.modules.execution.repository;

import com.tiba.pts.modules.execution.domain.entity.AttendanceRecord;
import com.tiba.pts.modules.execution.dto.response.StudentAttendanceResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
  long countByCourseSessionId(Long courseSessionId);

  List<AttendanceRecord> findByCourseSessionId(Long courseSessionId);

  @Query(
      """
    SELECT new com.tiba.pts.modules.execution.dto.response.StudentAttendanceResponse(
        e.id,
        s.firstName,
        s.lastName,
        s.studentCode,
        'PRESENT'
    )
    FROM ClassAssignment ca
    JOIN ca.enrollment e
    JOIN e.student s
    WHERE ca.classGroup.id = :classGroupId
    AND e.status IN ('VALIDATED', 'CONDITIONALLY_VALIDATED', 'SUSPENDED')
    AND CAST(e.createdDate AS date) <= :sessionDate
  """)
  List<StudentAttendanceResponse> findEligibleStudentsForCallSheet(
      @Param("classGroupId") Long classGroupId, @Param("sessionDate") LocalDate sessionDate);
}
