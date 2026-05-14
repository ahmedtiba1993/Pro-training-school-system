package com.tiba.pts.modules.user.mapper;

import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "role", ignore = true)
  User toEntity(UserCreateRequest request);
}
