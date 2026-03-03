package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.dto.ExamSessionDto;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ExamSessionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "term", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  ExamSession toEntity(ExamSessionDto request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "term", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(ExamSessionDto request, @MappingTarget ExamSession entity);

  ExamSessionDto toResponse(ExamSession entity);
}
