package com.tiba.pts.modules.execution.mapper;

import com.tiba.pts.modules.execution.domain.entity.CourseSession;
import com.tiba.pts.modules.execution.dto.request.CourseSessionRequest;
import com.tiba.pts.modules.execution.dto.request.CourseSessionUpdateRequest;
import com.tiba.pts.modules.execution.dto.response.CourseSessionResponse;
import com.tiba.pts.modules.profiles.mapper.RefTeacherSpecialtyMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {RefTeacherSpecialtyMapper.class},
    builder = @Builder(disableBuilder = true))
public interface CourseSessionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "attendanceSubmitted", ignore = true)
  @Mapping(target = "cancellationReason", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "subject", ignore = true)
  @Mapping(target = "teacher", ignore = true)
  @Mapping(target = "room", ignore = true)
  @Mapping(target = "attendances", ignore = true)
  CourseSession toEntity(CourseSessionRequest request);

  @Mapping(target = "classGroupId", source = "classGroup.id")
  @Mapping(target = "className", source = "classGroup.name")
  @Mapping(target = "subjectId", source = "subject.id")
  @Mapping(target = "subjectName", source = "subject.name")
  @Mapping(target = "teacherId", source = "teacher.id")
  @Mapping(
      target = "teacherName",
      expression =
          "java(courseSession.getTeacher() != null ? courseSession.getTeacher().getLastName() : null)")
  @Mapping(target = "roomId", source = "room.id")
  @Mapping(target = "roomName", source = "room.name")
  CourseSessionResponse toResponse(CourseSession courseSession);

  default String mapSpecialty(CourseSession entity) {
    if (entity == null
        || entity.getClassGroup() == null
        || entity.getClassGroup().getPromotion() == null
        || entity.getClassGroup().getPromotion().getTraining() == null) {
      return null;
    }
    var training = entity.getClassGroup().getPromotion().getTraining();
    String levelCode = training.getLevel() != null ? training.getLevel().getCode() : "";
    String specialtyLabel =
        training.getSpecialty() != null ? training.getSpecialty().getLabel() : "";
    return (levelCode + " " + specialtyLabel).trim();
  }

  default String formatSessionDate(java.time.LocalDate date) {
    if (date == null) {
      return null;
    }
    String raw =
        date.format(
            java.time.format.DateTimeFormatter.ofPattern(
                "EEEE dd MMMM yyyy", java.util.Locale.FRANCE));
    String[] words = raw.split(" ");
    if (words.length >= 4) {
      words[0] = words[0].substring(0, 1).toUpperCase() + words[0].substring(1);
      words[2] = words[2].substring(0, 1).toUpperCase() + words[2].substring(1);
    }
    return String.join(" ", words);
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "attendanceSubmitted", ignore = true)
  @Mapping(target = "cancellationReason", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "subject", ignore = true)
  @Mapping(target = "teacher", ignore = true)
  @Mapping(target = "room", ignore = true)
  @Mapping(target = "attendances", ignore = true)
  void updatePlannedEntity(
      CourseSessionUpdateRequest request, @org.mapstruct.MappingTarget CourseSession courseSession);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "sessionDate", ignore = true)
  @Mapping(target = "startTime", ignore = true)
  @Mapping(target = "endTime", ignore = true)
  @Mapping(target = "sessionType", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "lessonTitle", ignore = true)
  @Mapping(target = "courseContent", ignore = true)
  @Mapping(target = "attendanceSubmitted", ignore = true)
  @Mapping(target = "cancellationReason", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "subject", ignore = true)
  @Mapping(target = "teacher", ignore = true)
  @Mapping(target = "room", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "attendances", ignore = true)
  void updateInProgressEntity(
      CourseSessionUpdateRequest request, @org.mapstruct.MappingTarget CourseSession courseSession);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "sessionDate", ignore = true)
  @Mapping(target = "startTime", ignore = true)
  @Mapping(target = "endTime", ignore = true)
  @Mapping(target = "sessionType", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "attendanceSubmitted", ignore = true)
  @Mapping(target = "cancellationReason", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "subject", ignore = true)
  @Mapping(target = "teacher", ignore = true)
  @Mapping(target = "room", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "attendances", ignore = true)
  void updateCompletedEntity(
      CourseSessionUpdateRequest request, @org.mapstruct.MappingTarget CourseSession courseSession);
}
