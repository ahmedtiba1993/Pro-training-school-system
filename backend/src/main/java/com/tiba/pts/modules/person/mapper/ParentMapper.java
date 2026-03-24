package com.tiba.pts.modules.person.mapper;

import com.tiba.pts.modules.person.domain.entity.Parent;
import com.tiba.pts.modules.person.dto.request.ParentRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ParentMapper {
  Parent toEntity(ParentRequest request);
}
