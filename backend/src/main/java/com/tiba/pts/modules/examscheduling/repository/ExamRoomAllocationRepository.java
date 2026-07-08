package com.tiba.pts.modules.examscheduling.repository;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamRoomAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ExamRoomAllocationRepository extends JpaRepository<ExamRoomAllocation, Long> {

  boolean existsByRoomIdAndExamScheduleExamDateAndExamScheduleExamTimeSlotIdAndExamScheduleIdNot(
      Long roomId, LocalDate examDate, Long examTimeSlotId, Long scheduleId);

  boolean existsByRoomIdAndExamScheduleExamDateAndExamScheduleExamTimeSlotId(
      Long roomId, LocalDate examDate, Long examTimeSlotId);

  boolean existsByRoomIdAndExamScheduleId(Long roomId, Long examScheduleId);
}
