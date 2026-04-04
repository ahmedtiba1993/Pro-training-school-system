package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import com.tiba.pts.modules.academicyear.dto.request.HolidayRequest;
import com.tiba.pts.modules.academicyear.dto.response.HolidayResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HolidayMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "academicYear", source = "academicYearId", qualifiedByName = "idToAcademicYear")
  Holiday toEntity(HolidayRequest request);

  @Mapping(target = "academicYearId", source = "academicYear.id")
  HolidayResponse toResponse(Holiday entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "academicYear", source = "academicYearId", qualifiedByName = "idToAcademicYear")
  void updateEntityFromRequest(HolidayRequest request, @MappingTarget Holiday entity);

  @Named("idToAcademicYear")
  default AcademicYear idToAcademicYear(Long id) {
    if (id == null) return null;
    AcademicYear year = new AcademicYear();
    year.setId(id);
    return year;
  }
}
