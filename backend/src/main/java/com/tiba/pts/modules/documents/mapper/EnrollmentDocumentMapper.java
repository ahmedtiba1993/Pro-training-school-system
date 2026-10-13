package com.tiba.pts.modules.documents.mapper;

import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.documents.dto.request.EnrollmentDocumentRequest;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentResponse;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentSimpleResponse;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface EnrollmentDocumentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "levels", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "isActive", constant = "false")
  EnrollmentDocument toEntity(EnrollmentDocumentRequest request);

  EnrollmentDocumentResponse toResponse(EnrollmentDocument entity);

  EnrollmentDocumentResponse.LevelSummaryResponse levelToLevelSummaryResponse(Level level);

  EnrollmentDocumentSimpleResponse toSimpleResponse(EnrollmentDocument entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "levels", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  void updateEntityFromRequest(
      EnrollmentDocumentRequest request, @MappingTarget EnrollmentDocument entity);
}
