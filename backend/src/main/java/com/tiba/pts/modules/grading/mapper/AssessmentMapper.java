package com.tiba.pts.modules.grading.mapper;

import com.tiba.pts.modules.grading.domain.entity.Assessment;
import com.tiba.pts.modules.grading.dto.request.AssessmentRequest;
import com.tiba.pts.modules.grading.dto.response.AssessmentLookupResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AssessmentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "promotionSubject", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  Assessment toEntity(AssessmentRequest request);

  @Mapping(target = "subjectName", source = "promotionSubject.subject.name")
  AssessmentLookupResponse toResponse(Assessment assessment);
}
