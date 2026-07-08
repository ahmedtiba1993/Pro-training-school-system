package com.tiba.pts.modules.examscheduling.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamRoomAllocation;
import com.tiba.pts.modules.examscheduling.domain.entity.ExamSchedule;
import com.tiba.pts.modules.examscheduling.domain.enums.ExamTimetableStatus;
import com.tiba.pts.modules.examscheduling.dto.request.ExamRoomAllocationRequest;
import com.tiba.pts.modules.examscheduling.mapper.ExamRoomAllocationMapper;
import com.tiba.pts.modules.examscheduling.repository.ExamRoomAllocationRepository;
import com.tiba.pts.modules.examscheduling.repository.ExamScheduleRepository;
import com.tiba.pts.modules.room.domain.entity.Room;
import com.tiba.pts.modules.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamRoomAllocationService {

  private final ExamRoomAllocationRepository examRoomAllocationRepository;
  private final ExamScheduleRepository examScheduleRepository;
  private final RoomRepository roomRepository;
  private final ExamRoomAllocationMapper examRoomAllocationMapper;

  @Transactional
  public Long allocateRoom(Long scheduleId, ExamRoomAllocationRequest request) {
    ExamSchedule schedule =
        examScheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("EXAM_SCHEDULE_NOT_FOUND"));

    // Validation: Cannot modify/add rooms if timetable is published
    if (schedule.getExamTimetable().getStatus() == ExamTimetableStatus.PUBLISHED) {
      throw new BusinessValidationException("TIMETABLE_ALREADY_PUBLISHED");
    }

    // Validation: Cannot allocate room if the exam date is in the past
    if (schedule.getExamDate().isBefore(java.time.LocalDate.now())) {
      throw new BusinessValidationException("CANNOT_ALLOCATE_ROOM_FOR_PAST_EXAM");
    }

    Room room =
        roomRepository
            .findById(request.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND"));

    // Validation: Cannot allocate the same room twice to the same exam
    if (examRoomAllocationRepository.existsByRoomIdAndExamScheduleId(request.roomId(), scheduleId)) {
      throw new BusinessValidationException("ROOM_ALREADY_ALLOCATED_TO_THIS_EXAM");
    }

    // Validation: Verify if the room is already reserved at that date and time slot
    boolean roomOccupied =
        examRoomAllocationRepository
            .existsByRoomIdAndExamScheduleExamDateAndExamScheduleExamTimeSlotId(
                request.roomId(), schedule.getExamDate(), schedule.getExamTimeSlot().getId());

    if (roomOccupied) {
      throw new BusinessValidationException("ROOM_ALREADY_RESERVED_AT_THIS_TIME");
    }

    ExamRoomAllocation allocation = examRoomAllocationMapper.toEntity(request);
    allocation.setExamSchedule(schedule);
    allocation.setRoom(room);

    return examRoomAllocationRepository.save(allocation).getId();
  }

  @Transactional
  public void deallocateRoom(Long allocationId) {
    ExamRoomAllocation allocation =
        examRoomAllocationRepository
            .findById(allocationId)
            .orElseThrow(() -> new ResourceNotFoundException("ROOM_ALLOCATION_NOT_FOUND"));

    // Validation: Cannot modify/remove rooms if timetable is published
    if (allocation.getExamSchedule().getExamTimetable().getStatus() == ExamTimetableStatus.PUBLISHED) {
      throw new BusinessValidationException("TIMETABLE_ALREADY_PUBLISHED");
    }

    // Validation: Cannot deallocate room if the exam date is in the past
    if (allocation.getExamSchedule().getExamDate().isBefore(java.time.LocalDate.now())) {
      throw new BusinessValidationException("CANNOT_DEALLOCATE_ROOM_FOR_PAST_EXAM");
    }

    examRoomAllocationRepository.delete(allocation);
  }
}
