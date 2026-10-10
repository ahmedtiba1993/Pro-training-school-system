package com.tiba.pts.modules.specialty.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.dto.request.LevelRequest;
import com.tiba.pts.modules.specialty.dto.response.LevelResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LevelMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "trainings", ignore = true)
  Level toEntity(LevelRequest request);

  LevelResponse toResponse(Level level);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "trainings", ignore = true)
  void updateEntityFromRequest(LevelRequest request, @MappingTarget Level level);
}
