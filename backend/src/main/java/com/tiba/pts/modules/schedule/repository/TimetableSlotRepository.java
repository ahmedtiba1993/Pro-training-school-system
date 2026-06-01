package com.tiba.pts.modules.schedule.repository;

import com.tiba.pts.modules.schedule.domain.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tiba.pts.modules.schedule.domain.enums.DayOfWeek;
import java.util.List;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

  List<TimetableSlot> findAllByScheduleId(Long scheduleId);

  // Collision on the same schedule (same group) at the same time slot
  List<TimetableSlot> findByScheduleIdAndDayOfWeekAndTimeSlotDefinitionId(
      Long scheduleId, DayOfWeek dayOfWeek, Long timeSlotDefinitionId);

  // Teacher collision: same teacher, same day, same time slot (across all schedules)
  List<TimetableSlot> findByTeacherIdAndDayOfWeekAndTimeSlotDefinitionId(
      Long teacherId, DayOfWeek dayOfWeek, Long timeSlotDefinitionId);

  // Room collision: same room, same day, same time slot (across all schedules)
  List<TimetableSlot> findByRoomIdAndDayOfWeekAndTimeSlotDefinitionId(
      Long roomId, DayOfWeek dayOfWeek, Long timeSlotDefinitionId);

  // Retrieves all slots for a teacher (for the schedule view by teacher)
  List<TimetableSlot> findAllByTeacherId(Long teacherId);

  // Checks if a schedule contains at least one class slot
  boolean existsByScheduleId(Long scheduleId);
}
