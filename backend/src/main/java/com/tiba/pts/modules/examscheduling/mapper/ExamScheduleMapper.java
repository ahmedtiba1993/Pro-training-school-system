package com.tiba.pts.modules.examscheduling.mapper;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamSchedule;
import com.tiba.pts.modules.examscheduling.dto.request.ExamScheduleRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.tiba.pts.modules.examscheduling.dto.response.ExamScheduleResponse;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ExamScheduleMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "examTimetable", ignore = true)
  @Mapping(target = "assessment", ignore = true)
  @Mapping(target = "examTimeSlot", ignore = true)
  @Mapping(target = "roomAllocations", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  ExamSchedule toEntity(ExamScheduleRequest request);

  @Mapping(target = "timetableId", source = "examTimetable.id")
  @Mapping(target = "assessmentId", source = "assessment.id")
  @Mapping(target = "assessmentType", source = "assessment.assessmentType")
  @Mapping(target = "examTimeSlotId", source = "examTimeSlot.id")
  @Mapping(target = "examTimeSlotLabel", source = "examTimeSlot.label")
  @Mapping(target = "subjectName", source = "assessment.promotionSubject.subject.name")
  ExamScheduleResponse toResponse(ExamSchedule schedule);
}
