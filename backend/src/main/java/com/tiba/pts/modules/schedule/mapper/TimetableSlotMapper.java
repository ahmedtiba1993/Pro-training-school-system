package com.tiba.pts.modules.schedule.mapper;

import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.schedule.domain.entity.TimeSlotDefinition;
import com.tiba.pts.modules.schedule.domain.entity.TimetableSlot;
import com.tiba.pts.modules.schedule.dto.request.TimetableSlotRequest;
import com.tiba.pts.modules.schedule.dto.response.TimetableSlotDetailResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableSlotInfoResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TimetableSlotMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "schedule", ignore = true)
  @Mapping(target = "subject", ignore = true)
  @Mapping(target = "teacher", ignore = true)
  @Mapping(target = "room", ignore = true)
  TimetableSlot toEntity(TimetableSlotRequest request);

  @Mapping(target = "timeSlotDefinition.id", source = "timeSlotDefinition.id")
  @Mapping(target = "subject.id", source = "subject.id")
  @Mapping(target = "subject.name", source = "subject.name")
  @Mapping(target = "room.id", source = "room.id")
  @Mapping(target = "room.name", source = "room.name")
  @Mapping(
      target = "teacher",
      source = "teacher") // MapStruct will call the default method below
  TimetableSlotInfoResponse toInfoResponse(TimetableSlot entity);

  // Custom clean method to map Teacher to TeacherInfo
  default TimetableSlotInfoResponse.TeacherInfo mapTeacher(Teacher teacher) {
    if (teacher == null) {
      return null;
    }

    return TimetableSlotInfoResponse.TeacherInfo.builder()
        .id(teacher.getId())
        .name(
            teacher.getFirstName()
                + " "
                + teacher.getLastName()) // Concatenation is done cleanly here
        .build();
  }

  // Custom mapping for detailed slot response
  default TimetableSlotDetailResponse toDetailResponse(TimetableSlot entity) {
    if (entity == null) {
      return null;
    }

    Teacher teacher = entity.getTeacher();
    TimeSlotDefinition tsd = entity.getTimeSlotDefinition();

    return TimetableSlotDetailResponse.builder()
        .id(entity.getId())
        .subjectName(entity.getSubject() != null ? entity.getSubject().getName() : null)
        .teacherName(
            teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null)
        .roomName(entity.getRoom() != null ? entity.getRoom().getName() : null)
        .dayOfWeek(entity.getDayOfWeek() != null ? entity.getDayOfWeek().name() : null)
        .periodicity(entity.getPeriodicity())
        .timeSlot(
            tsd != null
                ? TimetableSlotDetailResponse.TimeSlotInfo.builder()
                    .id(tsd.getId())
                    .code(tsd.getCode())
                    .label(tsd.getLabel())
                    .startTime(tsd.getStartTime() != null ? tsd.getStartTime().toString() : null)
                    .endTime(tsd.getEndTime() != null ? tsd.getEndTime().toString() : null)
                    .build()
                : null)
        .build();
  }
}
