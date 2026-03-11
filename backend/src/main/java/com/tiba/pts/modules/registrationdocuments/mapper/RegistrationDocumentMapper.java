package com.tiba.pts.modules.registrationdocuments.mapper;

import com.tiba.pts.modules.registrationdocuments.domain.entity.RegistrationDocument;
import com.tiba.pts.modules.registrationdocuments.dto.RegistrationDocumentRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RegistrationDocumentMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "levels", ignore = true)
  RegistrationDocument toEntity(RegistrationDocumentRequest request);
}
