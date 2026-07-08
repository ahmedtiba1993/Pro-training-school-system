package com.tiba.pts.modules.examscheduling.mapper;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamRoomAllocation;
import com.tiba.pts.modules.examscheduling.dto.request.ExamRoomAllocationRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ExamRoomAllocationMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "examSchedule", ignore = true)
  @Mapping(target = "room", ignore = true)
  @Mapping(target = "invigilations", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  ExamRoomAllocation toEntity(ExamRoomAllocationRequest request);
}
