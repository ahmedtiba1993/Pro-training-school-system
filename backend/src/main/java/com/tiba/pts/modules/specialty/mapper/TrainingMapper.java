package com.tiba.pts.modules.specialty.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.dto.request.TrainingRequest;
import com.tiba.pts.modules.specialty.dto.response.TrainingResponse;
import com.tiba.pts.modules.specialty.dto.response.TrainingTypeCountResponse;
import com.tiba.pts.modules.specialty.repository.projection.TrainingTypeCountProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TrainingMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "level", ignore = true)
  @Mapping(target = "specialty", ignore = true)
  Training toEntity(TrainingRequest requestDTO);

  @Mapping(source = "level.id", target = "levelId")
  @Mapping(source = "level.code", target = "levelCode")
  @Mapping(source = "level.label", target = "levelLabel")
  @Mapping(source = "specialty.id", target = "specialtyId")
  @Mapping(source = "specialty.code", target = "specialtyCode")
  @Mapping(source = "specialty.label", target = "specialtyLabel")
  TrainingResponse toResponse(Training entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "level", ignore = true)
  @Mapping(target = "specialty", ignore = true)
  @Mapping(
      target = "createdDate",
      ignore = true)
  void updateEntityFromRequest(TrainingRequest requestDTO, @MappingTarget Training training);

  TrainingTypeCountResponse toTrainingTypeCountResponse(TrainingTypeCountProjection projection);

  List<TrainingTypeCountResponse> toTrainingTypeCountResponseList(
      List<TrainingTypeCountProjection> projections);
}
