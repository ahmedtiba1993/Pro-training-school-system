package com.tiba.pts.modules.trainingsession.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.trainingsession.domain.entity.ContinuousPromotion;
import com.tiba.pts.modules.trainingsession.dto.request.ContinuousPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.ContinuousPromotionResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContinuousPromotionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "training", ignore = true)
  ContinuousPromotion toEntity(ContinuousPromotionRequest dto);

  @Mapping(source = "training.id", target = "trainingId")
  @Mapping(source = "training", target = "trainingLabel")
  ContinuousPromotionResponse toResponse(ContinuousPromotion entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "training", ignore = true)
  void updateEntityFromRequest(
      ContinuousPromotionRequest request, @MappingTarget ContinuousPromotion entity);

  // Custom method to concatenate Level + Specialty
  default String mapTrainingLabel(Training training) {
    if (training == null) {
      return null;
    }
    String levelStr =
        (training.getLevel() != null && training.getLevel().getLabel() != null)
            ? training.getLevel().getLabel()
            : "";
    String specialtyStr =
        (training.getSpecialty() != null && training.getSpecialty().getLabel() != null)
            ? training.getSpecialty().getLabel()
            : "";
    return (levelStr + " " + specialtyStr).trim();
  }
}
