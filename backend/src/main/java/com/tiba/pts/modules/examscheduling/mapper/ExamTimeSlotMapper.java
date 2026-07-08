package com.tiba.pts.modules.examscheduling.mapper;

import com.tiba.pts.modules.examscheduling.domain.entity.ExamTimeSlot;
import com.tiba.pts.modules.examscheduling.dto.request.ExamTimeSlotRequest;
import com.tiba.pts.modules.examscheduling.dto.response.ExamTimeSlotResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ExamTimeSlotMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  ExamTimeSlot toEntity(ExamTimeSlotRequest request);

  ExamTimeSlotResponse toResponse(ExamTimeSlot entity);
}
