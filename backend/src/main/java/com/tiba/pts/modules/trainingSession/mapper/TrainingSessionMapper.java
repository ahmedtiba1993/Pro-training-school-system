package com.tiba.pts.modules.trainingSession.mapper;

import com.tiba.pts.modules.trainingSession.domain.entity.TrainingSession;
import com.tiba.pts.modules.trainingSession.dto.TrainingSessionRequest;
import com.tiba.pts.modules.trainingSession.dto.TrainingSessionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TrainingSessionMapper {

  @Mapping(target = "academicYear.id", source = "academicYearId")
  @Mapping(target = "level.id", source = "levelId")
  @Mapping(target = "specialty.id", source = "specialtyId")
  @Mapping(target = "enrolledCount", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  TrainingSession toEntity(TrainingSessionRequest request);

  @Mapping(target = "academicYearId", source = "academicYear.id")
  @Mapping(target = "levelId", source = "level.id")
  @Mapping(target = "specialtyId", source = "specialty.id")
  @Mapping(target = "academicYearLabel", source = "academicYear.label")
  @Mapping(target = "levelCode", source = "level.code")
  @Mapping(target = "specialtyCode", source = "specialty.code")
  TrainingSessionResponse toResponse(TrainingSession entity);

  @Mapping(target = "academicYear.id", source = "academicYearId")
  @Mapping(target = "level.id", source = "levelId")
  @Mapping(target = "specialty.id", source = "specialtyId")
  @Mapping(target = "enrolledCount", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  void updateEntityFromRequest(
      TrainingSessionRequest request, @MappingTarget TrainingSession entity);
}
