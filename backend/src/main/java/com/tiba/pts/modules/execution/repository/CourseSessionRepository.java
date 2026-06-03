package com.tiba.pts.modules.execution.repository;

import com.tiba.pts.modules.execution.domain.entity.CourseSession;
import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

  @Query(
      """
      SELECT cs
      FROM CourseSession cs
      WHERE cs.sessionDate = :date
      AND cs.status != com.tiba.pts.modules.execution.domain.enums.SessionStatus.CANCELED
      AND (cs.startTime < :newEndTime AND cs.endTime > :newStartTime)
      AND (
          cs.teacher.id = :teacherId
          OR cs.classGroup.id = :classGroupId
          OR (cs.room.id = :roomId AND :roomId IS NOT NULL)
      )
  """)
  List<CourseSession> findCollidingSessions(
      @Param("date") LocalDate date,
      @Param("newStartTime") LocalTime newStartTime,
      @Param("newEndTime") LocalTime newEndTime,
      @Param("teacherId") Long teacherId,
      @Param("classGroupId") Long classGroupId,
      @Param("roomId") Long roomId);

  @Query(
      """
      SELECT cs
      FROM CourseSession cs
      WHERE cs.id != :id
      AND cs.sessionDate = :date
      AND cs.status != com.tiba.pts.modules.execution.domain.enums.SessionStatus.CANCELED
      AND (cs.startTime < :newEndTime AND cs.endTime > :newStartTime)
      AND (
          cs.teacher.id = :teacherId
          OR cs.classGroup.id = :classGroupId
          OR (cs.room.id = :roomId AND :roomId IS NOT NULL)
      )
  """)
  List<CourseSession> findCollidingSessionsExcludingId(
      @Param("id") Long id,
      @Param("date") LocalDate date,
      @Param("newStartTime") LocalTime newStartTime,
      @Param("newEndTime") LocalTime newEndTime,
      @Param("teacherId") Long teacherId,
      @Param("classGroupId") Long classGroupId,
      @Param("roomId") Long roomId);

  @Query(
      value =
          """
      SELECT cs
      FROM CourseSession cs
      LEFT JOIN FETCH cs.classGroup cg
      LEFT JOIN FETCH cg.promotion p
      LEFT JOIN FETCH p.training t
      LEFT JOIN FETCH t.level l
      LEFT JOIN FETCH t.specialty sp
      LEFT JOIN FETCH cs.subject s
      LEFT JOIN FETCH cs.teacher te
      LEFT JOIN FETCH cs.room r
      WHERE (CAST(:startTime AS time) IS NULL OR cs.startTime >= :startTime)
      AND (CAST(:endTime AS time) IS NULL OR cs.endTime <= :endTime)
      AND (CAST(:promotionId AS long) IS NULL OR p.id = :promotionId)
      AND (CAST(:teacherId AS long) IS NULL OR te.id = :teacherId)
      AND (CAST(:subjectId AS long) IS NULL OR s.id = :subjectId)
      AND (CAST(:status AS string) IS NULL OR cs.status = :status)
      AND (CAST(:isAttendanceSubmitted AS boolean) IS NULL OR cs.isAttendanceSubmitted = :isAttendanceSubmitted)
      AND (CAST(:sessionType AS string) IS NULL OR cs.sessionType = :sessionType)
  """,
      countQuery =
          """
      SELECT COUNT(cs)
      FROM CourseSession cs
      LEFT JOIN cs.classGroup cg
      LEFT JOIN cg.promotion p
      WHERE (CAST(:startTime AS time) IS NULL OR cs.startTime >= :startTime)
      AND (CAST(:endTime AS time) IS NULL OR cs.endTime <= :endTime)
      AND (CAST(:promotionId AS long) IS NULL OR p.id = :promotionId)
      AND (CAST(:teacherId AS long) IS NULL OR cs.teacher.id = :teacherId)
      AND (CAST(:subjectId AS long) IS NULL OR cs.subject.id = :subjectId)
      AND (CAST(:status AS string) IS NULL OR cs.status = :status)
      AND (CAST(:isAttendanceSubmitted AS boolean) IS NULL OR cs.isAttendanceSubmitted = :isAttendanceSubmitted)
      AND (CAST(:sessionType AS string) IS NULL OR cs.sessionType = :sessionType)
  """)
  org.springframework.data.domain.Page<CourseSession> findAllFiltered(
      @Param("startTime") LocalTime startTime,
      @Param("endTime") LocalTime endTime,
      @Param("promotionId") Long promotionId,
      @Param("teacherId") Long teacherId,
      @Param("subjectId") Long subjectId,
      @Param("status") com.tiba.pts.modules.execution.domain.enums.SessionStatus status,
      @Param("isAttendanceSubmitted") Boolean isAttendanceSubmitted,
      @Param("sessionType") com.tiba.pts.modules.execution.domain.enums.SessionType sessionType,
      org.springframework.data.domain.Pageable pageable);

  @Query(
      "SELECT COUNT(cs) FROM CourseSession cs WHERE cs.sessionDate = :date AND cs.status != :status")
  long countBySessionDateAndStatusNot(
      @Param("date") LocalDate date, @Param("status") SessionStatus status);

  @Query(
      "SELECT COUNT(cs) FROM CourseSession cs WHERE cs.sessionDate = :date AND cs.status != :status AND cs.isAttendanceSubmitted = false")
  long countBySessionDateAndStatusNotAndIsAttendanceSubmittedFalse(
      @Param("date") LocalDate date, @Param("status") SessionStatus status);

  @Query(
      "SELECT COUNT(cs) FROM CourseSession cs WHERE cs.sessionDate = :date AND cs.status = :status")
  long countBySessionDateAndStatus(
      @Param("date") LocalDate date, @Param("status") SessionStatus status);

  @Query(
      """
      SELECT cs
      FROM CourseSession cs
      LEFT JOIN FETCH cs.classGroup cg
      LEFT JOIN FETCH cg.promotion p
      LEFT JOIN FETCH p.training t
      LEFT JOIN FETCH t.level l
      LEFT JOIN FETCH t.specialty sp
      LEFT JOIN FETCH cs.subject s
      LEFT JOIN FETCH cs.teacher te
      LEFT JOIN FETCH cs.room r
      WHERE cs.id = :id
  """)
  Optional<CourseSession> findByIdWithRelations(@Param("id") Long id);
}
