package com.tiba.pts.modules.user.mapper;

import com.tiba.pts.modules.user.dto.request.UserCreateRequest;
import com.tiba.pts.modules.user.domain.entity.User;
import com.tiba.pts.modules.user.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "role", ignore = true)
  User toEntity(UserCreateRequest request);

  @Mapping(source = "person.id", target = "personId")
  @Mapping(source = "person.firstName", target = "firstName")
  @Mapping(source = "person.lastName", target = "lastName")
  UserResponse toResponse(User user);
}
