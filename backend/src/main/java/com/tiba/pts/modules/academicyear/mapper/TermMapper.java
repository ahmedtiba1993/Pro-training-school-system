package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.Term;
import com.tiba.pts.modules.academicyear.dto.TermDto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TermMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "academicYear", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Term toEntity(TermDto request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "academicYear", ignore = true)
  @Mapping(target = "examSessions", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(TermDto request, @MappingTarget Term entity);

  TermDto toResponse(Term entity);
}
