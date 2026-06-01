package com.tiba.pts.modules.schedule.mapper;

import com.tiba.pts.modules.schedule.domain.entity.Schedule;
import com.tiba.pts.modules.schedule.dto.request.ScheduleRequest;
import com.tiba.pts.modules.schedule.dto.request.ScheduleUpdateRequest;
import com.tiba.pts.modules.schedule.dto.response.ScheduleInfoResponse;
import com.tiba.pts.modules.schedule.dto.response.ScheduleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ScheduleMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "period", ignore = true)
  Schedule toEntity(ScheduleRequest request);

  @Mapping(target = "classGroupId", source = "classGroup.id")
  @Mapping(target = "classGroupName", source = "classGroup.name")
  @Mapping(target = "periodId", source = "period.id")
  @Mapping(target = "periodLabel", source = "period.label")
  @Mapping(target = "trainingName", expression = "java(buildTrainingName(entity))")
  ScheduleResponse toResponse(Schedule entity);

  @Mapping(target = "promotionId", source = "classGroup.promotion.id")
  ScheduleInfoResponse toScheduleInfoResponse(Schedule entity);

  /**
   * Updates only the authorized fields of the existing Schedule. Implicitly ignores the ID,
   * status, class, and period.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "classGroup", ignore = true)
  @Mapping(target = "period", ignore = true)
  void updateEntityFromRequest(ScheduleUpdateRequest request, @MappingTarget Schedule entity);

  default String buildTrainingName(Schedule entity) {
    if (entity == null
        || entity.getClassGroup() == null
        || entity.getClassGroup().getPromotion() == null
        || entity.getClassGroup().getPromotion().getTraining() == null) {
      return null;
    }
    var training = entity.getClassGroup().getPromotion().getTraining();
    String levelCode = training.getLevel() != null ? training.getLevel().getCode() : "";
    String specialtyLabel =
        training.getSpecialty() != null ? training.getSpecialty().getLabel() : "";
    return (levelCode + " " + specialtyLabel).trim();
  }
}
