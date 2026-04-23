package com.tiba.pts.modules.documents.mapper;

import com.tiba.pts.modules.documents.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.documents.dto.request.EnrollmentDocumentRequest;
import com.tiba.pts.modules.documents.dto.response.EnrollmentDocumentResponse;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import org.mapstruct.Mapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface EnrollmentDocumentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "levels", ignore = true)
  EnrollmentDocument toEntity(EnrollmentDocumentRequest request);

  EnrollmentDocumentResponse toResponse(EnrollmentDocument entity);

  EnrollmentDocumentResponse.LevelSummaryResponse levelToLevelSummaryResponse(Level level);
}
