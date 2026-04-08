package com.tiba.pts.modules.specialty.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.dto.request.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.response.SpecialtyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {

  @Mapping(target = "id", ignore = true)
  Specialty toEntity(SpecialtyRequest request);

  SpecialtyResponse toResponse(Specialty specialty);

  List<SpecialtyResponse> toResponseList(List<Specialty> specialties);
}
