package com.tiba.pts.modules.specialty.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.dto.SpecialtyRequest;
import com.tiba.pts.modules.specialty.dto.SpecialtyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "associatedLevels", expression = "java(new java.util.HashSet<>())")
  Specialty toEntity(SpecialtyRequest request);

  List<SpecialtyResponse> toResponseList(List<Specialty> specialties);
}
