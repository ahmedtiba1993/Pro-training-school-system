package com.tiba.pts.modules.specialty.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.dto.request.LevelRequest;
import com.tiba.pts.modules.specialty.dto.response.LevelResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LevelMapper {

  Level toEntity(LevelRequest request);

  LevelResponse toReponse(Level level);

  @Mapping(target = "id", ignore = true)
  void updateEntityFromDto(LevelRequest request, @MappingTarget Level level);
}
