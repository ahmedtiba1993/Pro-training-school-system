package com.tiba.pts.modules.schedule.mapper;

import com.tiba.pts.modules.schedule.domain.entity.TimeSlotDefinition;
import com.tiba.pts.modules.schedule.dto.request.TimeSlotDefinitionRequest;
import com.tiba.pts.modules.schedule.dto.response.TimeSlotDefinitionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TimeSlotDefinitionMapper {

  @Mapping(target = "id", ignore = true)
  TimeSlotDefinition toEntity(TimeSlotDefinitionRequest request);

  TimeSlotDefinitionResponse toResponse(TimeSlotDefinition entity);

  @Mapping(target = "id", ignore = true)
  void updateEntityFromRequest(
      TimeSlotDefinitionRequest request, @MappingTarget TimeSlotDefinition entity);
}
