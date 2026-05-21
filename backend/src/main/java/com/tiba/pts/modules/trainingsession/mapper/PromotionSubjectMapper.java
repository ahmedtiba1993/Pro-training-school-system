package com.tiba.pts.modules.trainingsession.mapper;

import com.tiba.pts.modules.trainingsession.domain.entity.PromotionSubject;
import com.tiba.pts.modules.trainingsession.dto.response.PromotionSubjectResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PromotionSubjectMapper {

  @Mapping(source = "subject.name", target = "subjectName")
  @Mapping(source = "subject.theoryHours", target = "theoryHours")
  @Mapping(source = "subject.practicalHours", target = "practicalHours")
  @Mapping(source = "subject.totalHours", target = "totalHours")
  PromotionSubjectResponse toResponse(PromotionSubject promotionSubject);

  /**
   * Maps a list of entities to a list of DTOs. Ideal for retrieving the list of subjects for a
   * specific promotion.
   */
  List<PromotionSubjectResponse> toResponseList(List<PromotionSubject> promotionSubjects);
}
