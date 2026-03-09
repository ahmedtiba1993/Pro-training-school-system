package com.tiba.pts.modules.specialty.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.dto.LevelDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LevelMapper {

  Level toEntity(LevelDto levelDto);

  LevelDto toReponse(Level level);
}
