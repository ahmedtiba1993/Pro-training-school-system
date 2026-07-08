package com.tiba.pts.modules.examscheduling.mapper;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimetable;
import com.tiba.pts.modules.examscheduling.dto.request.ExamTimetableRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamTimetableResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ExamTimetableMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "period", ignore = true)
  @Mapping(target = "examSession", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  ExamTimetable toEntity(ExamTimetableRequest request);

  @Mapping(target = "classGroupId", source = "classGroup.id")
  @Mapping(target = "classGroupName", source = "classGroup.name")
  @Mapping(target = "periodId", source = "period.id")
  @Mapping(target = "periodLabel", source = "period.label")
  @Mapping(target = "examSessionId", source = "examSession.id")
  @Mapping(target = "examSessionLabel", source = "examSession.label")
  @Mapping(target = "sessionType", source = "examSession.sessionType")
  ExamTimetableResponse toResponse(ExamTimetable entity);
}
