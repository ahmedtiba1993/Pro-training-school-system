package com.tiba.pts.modules.classmanagement.mapper;

import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.dto.request.ClassGroupRequest;
import com.tiba.pts.modules.classmanagement.dto.response.ActiveClassGroupResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassGroupDetailResponse;
import com.tiba.pts.modules.classmanagement.dto.response.ClassGroupResponse;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ClassGroupMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "assignments", ignore = true)
  @Mapping(target = "promotion", ignore = true)
  ClassGroup toEntity(ClassGroupRequest request);

  @Mapping(target = "promotionId", source = "promotion.id")
  @Mapping(target = "promotionName", source = "entity", qualifiedByName = "resolvePromotionName")
  // Mapping configuration for the polymorphic type of the promotion
  @Mapping(target = "promotionType", source = "entity", qualifiedByName = "resolvePromotionType")
  ClassGroupResponse toResponse(ClassGroup entity);

  @Mapping(target = "className", source = "entity.name")
  @Mapping(target = "promotionName", source = "entity.promotion.name")
  @Mapping(target = "specialityName", source = "entity", qualifiedByName = "buildSpecialityName")
  ClassGroupDetailResponse toDetailResponse(ClassGroup entity);

  @Mapping(source = "promotion.training.trainingType", target = "trainingType")
  ActiveClassGroupResponse toActiveResponse(ClassGroup entity);

  List<ActiveClassGroupResponse> toActiveResponseList(List<ClassGroup> entities);

  /**
   * Dynamically resolves the promotion name in the Mapper. Leaves the Promotion entity totally
   * unchanged.
   */
  @Named("resolvePromotionName")
  default String resolvePromotionName(ClassGroup classGroup) {
    if (classGroup == null || classGroup.getPromotion() == null) {
      return null;
    }

    Training training = classGroup.getPromotion().getTraining();
    if (training == null) {
      return null;
    }

    String levelCode = training.getLevel() != null ? training.getLevel().getCode() : "";
    String specialtyLabel =
        training.getSpecialty() != null ? training.getSpecialty().getLabel() : "";

    if (levelCode.isEmpty() && specialtyLabel.isEmpty()) {
      return null;
    }

    return levelCode + " - " + specialtyLabel;
  }

  /** Polymorphically resolves the promotion type using Pattern Matching */
  @Named("resolvePromotionType")
  default String resolvePromotionType(ClassGroup classGroup) {
    if (classGroup == null || classGroup.getPromotion() == null) {
      return null;
    }

    Promotion promotion = classGroup.getPromotion();

    if (promotion.getClass().getSimpleName().contains("Accredited")) {
      return "ACCREDITED";
    } else if (promotion.getClass().getSimpleName().contains("Accelerated")) {
      return "ACCELERATED";
    } else if (promotion.getClass().getSimpleName().contains("Continuous")) {
      return "CONTINUOUS";
    }

    return "STANDARD";
  }

  /** Custom method to safely concatenate: level.code + "-" + specialty.label */
  @Named("buildSpecialityName")
  default String buildSpecialityName(ClassGroup entity) {
    if (entity == null || entity.getPromotion() == null) {
      return null;
    }

    Training training = entity.getPromotion().getTraining();
    if (training == null) {
      return null;
    }

    String levelCode = training.getLevel() != null ? training.getLevel().getCode() : "";
    String specialtyLabel =
        training.getSpecialty() != null ? training.getSpecialty().getLabel() : "";

    if (levelCode.isEmpty() && specialtyLabel.isEmpty()) {
      return null;
    }

    return levelCode + "-" + specialtyLabel;
  }
}
