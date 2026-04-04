package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.dto.request.AcademicYearRequest;
import com.tiba.pts.modules.academicyear.dto.response.AcademicYearResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AcademicYearMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "periods", ignore = true)
  @Mapping(target = "holidays", ignore = true)
  AcademicYear toEntity(AcademicYearRequest dto);

  AcademicYearResponse toResponse(AcademicYear entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isActive", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "periods", ignore = true)
  @Mapping(target = "holidays", ignore = true)
  void updateEntityFromRequest(AcademicYearRequest request, @MappingTarget AcademicYear entity);
}
