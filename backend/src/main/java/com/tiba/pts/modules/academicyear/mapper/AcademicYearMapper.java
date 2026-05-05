package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.dto.request.AcademicYearRequest;
import com.tiba.pts.modules.academicyear.dto.response.AcademicYearResponse;
import com.tiba.pts.modules.academicyear.dto.response.ActiveAcademicYearResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AcademicYearMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isDefault", ignore = true)
  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "periods", ignore = true)
  @Mapping(target = "holidays", ignore = true)
  AcademicYear toEntity(AcademicYearRequest request);

  AcademicYearResponse toResponse(AcademicYear entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isDefault", ignore = true)
  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "periods", ignore = true)
  @Mapping(target = "holidays", ignore = true)
  void updateEntityFromRequest(AcademicYearRequest request, @MappingTarget AcademicYear entity);

  ActiveAcademicYearResponse toActiveResponse(
      AcademicYear year,
      long remainingDaysInYear,
      String currentPeriodLabel,
      int periodProgress,
      long remainingDaysInCurrentPeriod);
}
