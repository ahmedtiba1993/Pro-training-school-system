package com.tiba.pts.modules.trainingsession.mapper;

import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AccreditedPromotionMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "training", ignore = true)
  @Mapping(target = "academicYear", ignore = true)
  AccreditedPromotion toEntity(AccreditedPromotionRequest dto);

  @Mapping(source = "training.id", target = "trainingId")
  @Mapping(source = "training", target = "trainingLabel")
  @Mapping(source = "training.specialty.label", target = "specialityLabel")
  @Mapping(source = "academicYear.id", target = "academicYearId")
  @Mapping(source = "academicYear.label", target = "academicYearLabel")
  AccreditedPromotionResponse toResponse(AccreditedPromotion entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "training", ignore = true)
  @Mapping(target = "academicYear", ignore = true)
  void updateEntityFromRequest(
      AccreditedPromotionRequest request, @MappingTarget AccreditedPromotion entity);

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
