package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.dto.request.PeriodRequest;
import com.tiba.pts.modules.academicyear.dto.response.PeriodResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PeriodMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "sessions", ignore = true)
  @Mapping(target = "academicYear", source = "academicYearId")
  Period toEntity(PeriodRequest request);

  @Mapping(target = "academicYearId", source = "academicYear.id")
  PeriodResponse toResponse(Period period);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "sessions", ignore = true)
  @Mapping(target = "academicYear", ignore = true)
  void updatePeriodFromRequest(PeriodRequest request, @MappingTarget Period period);

  // Helper for MapStruct: Transform a Long into a Proxy object (avoids a SQL query)
  // @Named("idToAcademicYear")
  default AcademicYear idToAcademicYear(Long id) {
    if (id == null) return null;
    AcademicYear year = new AcademicYear();
    year.setId(id);
    return year;
  }
}
