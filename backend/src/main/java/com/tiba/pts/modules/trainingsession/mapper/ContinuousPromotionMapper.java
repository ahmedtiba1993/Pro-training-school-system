package com.tiba.pts.modules.trainingsession.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.trainingsession.domain.entity.ContinuousPromotion;
import com.tiba.pts.modules.trainingsession.dto.request.ContinuousPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.ContinuousPromotionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContinuousPromotionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "enrollmentCount", ignore = true)
  @Mapping(target = "training", ignore = true)
  ContinuousPromotion toEntity(ContinuousPromotionRequest request);

  @Mapping(source = "training.id", target = "trainingId")
  @Mapping(source = "training", target = "trainingLabel")
  @Mapping(source = "training.specialty.label", target = "specialityLabel")
  ContinuousPromotionResponse toResponse(ContinuousPromotion entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "enrollmentCount", ignore = true)
  @Mapping(target = "training", ignore = true)
  void updateEntityFromRequest(
      ContinuousPromotionRequest request, @MappingTarget ContinuousPromotion entity);

  default String mapTrainingLabel(Training training) {
    if (training == null) return null;
    String levelStr =
        (training.getLevel() != null && training.getSpecialty().getLabel() != null)
            ? training.getLevel().getCode()
            : "";
    String specialtyStr =
        (training.getSpecialty() != null && training.getSpecialty().getLabel() != null)
            ? training.getSpecialty().getCode()
            : "";
    return (levelStr + " " + specialtyStr).trim();
  }
}
