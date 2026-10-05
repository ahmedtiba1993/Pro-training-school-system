package com.tiba.pts.modules.auth.mapper;

import com.tiba.pts.modules.auth.dto.UserInfo;
import com.tiba.pts.modules.user.domain.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

/**
 * Mapping Entity -> DTO pour les informations utilisateur de session.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AuthMapper {

  UserInfo toUserInfo(User user);
}
