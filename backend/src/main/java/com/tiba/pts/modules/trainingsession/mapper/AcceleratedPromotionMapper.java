package com.tiba.pts.modules.trainingsession.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.trainingsession.domain.entity.AcceleratedPromotion;
import com.tiba.pts.modules.trainingsession.dto.request.AcceleratedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AcceleratedPromotionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AcceleratedPromotionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "training", ignore = true)
  AcceleratedPromotion toEntity(AcceleratedPromotionRequest dto);

  @Mapping(source = "training.id", target = "trainingId")
  @Mapping(source = "training", target = "trainingLabel")
  @Mapping(source = "training.specialty.label", target = "specialityLabel")
  AcceleratedPromotionResponse toResponse(AcceleratedPromotion entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "training", ignore = true)
  void updateEntityFromRequest(
      AcceleratedPromotionRequest request, @MappingTarget AcceleratedPromotion entity);

  // Custom method to concatenate Level + Specialty
  default String mapTrainingLabel(Training training) {
    if (training == null) {
      return null;
    }
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
