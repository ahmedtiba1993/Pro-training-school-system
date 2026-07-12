package com.tiba.pts.modules.schedule.repository;

import com.tiba.pts.modules.schedule.domain.entity.Schedule;
import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

  // For Accredited Training
  boolean existsByClassGroupIdAndPeriodId(Long classGroupId, Long periodId);

  // For Continuous / Accelerated Training
  boolean existsByClassGroupIdAndPeriodIsNull(Long classGroupId);

  // Optimized SQL join to load the entire dependency tree required by your new DTO
  @EntityGraph(
      attributePaths = {
        "classGroup.promotion.training.level",
        "classGroup.promotion.training.specialty",
        "period"
      })
  List<Schedule> findAllByStatus(ScheduleStatus status);

  // ACTIVE uniqueness for Accredited Training: same ClassGroup + same Period
  boolean existsByClassGroupIdAndPeriodIdAndStatus(
      Long classGroupId, Long periodId, ScheduleStatus status);

  // ACTIVE uniqueness for Continuous/Accelerated Training: same ClassGroup (period is null)
  boolean existsByClassGroupIdAndPeriodIsNullAndStatus(Long classGroupId, ScheduleStatus status);

  @EntityGraph(
      attributePaths = {
        "classGroup",
        "classGroup.promotion.training.level",
        "classGroup.promotion.training.specialty",
        "period"
      })
  @Query("""
      SELECT s FROM Schedule s
      WHERE s.status = com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus.ACTIVE
        AND s.classGroup.id IN (
            SELECT ca.classGroup.id FROM ClassAssignment ca
            WHERE ca.enrollment.student.id = :studentId
              AND ca.classGroup.status = com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus.ACTIVE
        )
  """)
  List<Schedule> findSchedulesByStudentId(@Param("studentId") Long studentId);

  @EntityGraph(
      attributePaths = {
        "classGroup",
        "classGroup.promotion.training.level",
        "classGroup.promotion.training.specialty",
        "period"
      })
  @Query("""
      SELECT DISTINCT s FROM Schedule s
      JOIN TimetableSlot ts ON ts.schedule = s
      WHERE s.status = com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus.ACTIVE
        AND ts.teacher.id = :teacherId
  """)
  List<Schedule> findSchedulesByTeacherId(@Param("teacherId") Long teacherId);
}
